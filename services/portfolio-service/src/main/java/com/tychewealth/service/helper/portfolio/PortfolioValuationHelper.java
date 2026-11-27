package com.tychewealth.service.helper.portfolio;

import static com.tychewealth.constants.CommonConstants.ERROR;
import static com.tychewealth.constants.CommonConstants.NAME;
import static com.tychewealth.error.handler.ErrorDefinition.PORTFOLIO_VALUATION_EXCHANGE_RATE_MISSING;
import static com.tychewealth.error.handler.ErrorDefinition.PORTFOLIO_VALUATION_MARKET_PRICE_MISSING;

import com.tychewealth.dto.portfolio.PortfolioValuationExchangeRateDto;
import com.tychewealth.dto.portfolio.PortfolioValuationPointDto;
import com.tychewealth.dto.portfolio.PortfolioValuationPriceObservationDto;
import com.tychewealth.dto.portfolio.PortfolioValuationPriceSource;
import com.tychewealth.dto.portfolio.PortfolioValuationResponseDto;
import com.tychewealth.entity.AssetVariationEntity;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.enums.AssetVariationTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.error.exception.PortfolioException;
import com.tychewealth.mapper.portfolio.PortfolioMapper;
import com.tychewealth.marketdataclient.dto.ExchangeRate;
import com.tychewealth.marketdataclient.dto.LatestMarketPrice;
import com.tychewealth.marketdataclient.dto.PriceCheckpoint;
import com.tychewealth.model.PortfolioPositionSnapshot;
import com.tychewealth.model.PortfolioValuationEndPrices;
import com.tychewealth.model.PortfolioValuationPosition;
import com.tychewealth.repository.AssetVariationRepository;
import com.tychewealth.service.helper.marketdata.PortfolioValuationMarketDataHelper;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Provides valuation-specific date handling. */
@Component
@RequiredArgsConstructor
public class PortfolioValuationHelper {

  private final PortfolioMapper portfolioMapper;
  private final AssetVariationRepository assetVariationRepository;
  private final PortfolioValuationMarketDataHelper portfolioValuationMarketDataHelper;

  public Instant resolveTo(Instant to) {
    return to == null ? Instant.now() : to;
  }

  public Instant resolveFrom(Instant from, PortfolioEntity portfolio) {
    return from == null ? portfolio.getCreatedAt().toInstant(ZoneOffset.UTC) : from;
  }

  public PortfolioValuationResponseDto valuePortfolio(
      Long portfolioId,
      PortfolioEntity portfolio,
      Instant from,
      Instant to,
      boolean useLatestPricesAtEnd) {
    var rangeStart = from.atOffset(ZoneOffset.UTC).toLocalDateTime();
    var rangeEnd = to.atOffset(ZoneOffset.UTC).toLocalDateTime();
    List<AssetVariationEntity> initialStates =
        assetVariationRepository.findLatestStatePerAssetAtOrBefore(portfolioId, rangeStart);
    List<AssetVariationEntity> rangeVariations =
        assetVariationRepository.findPortfolioVariationsAfterAndThrough(
            portfolioId, rangeStart, rangeEnd);
    List<AssetVariationEntity> allVariations = new ArrayList<>(initialStates);
    allVariations.addAll(rangeVariations);
    List<String> symbols = symbolsForVariations(allVariations);

    List<PriceCheckpoint> sampledHistory =
        portfolioValuationMarketDataHelper.retrieveSampledPriceHistory(symbols, from, to);
    List<Instant> timestamps = valuationTimestamps(from, to, rangeVariations, sampledHistory);
    List<PortfolioPositionSnapshot> snapshots =
        reconstructPositions(initialStates, rangeVariations, timestamps);
    List<PriceCheckpoint> priceHistory =
        portfolioValuationMarketDataHelper.retrievePriceHistory(
            symbols, snapshots.stream().map(PortfolioPositionSnapshot::timestamp).toList());
    List<String> endSymbols = symbolsForPositions(snapshots.getLast().positions());
    PortfolioValuationEndPrices endPrices =
        portfolioValuationMarketDataHelper.retrieveEndPrices(endSymbols, to, useLatestPricesAtEnd);
    CurrencyCodeEnum baseCurrency = portfolio.getBaseCurrency();
    Map<String, ExchangeRate> exchangeRates =
        portfolioValuationMarketDataHelper.retrieveExchangeRates(
            requiredCurrencyPairs(snapshots, baseCurrency));
    List<PortfolioValuationPointDto> points =
        calculateValuationPoints(
            snapshots,
            priceHistory,
            endPrices.latestPrices(),
            endPrices.checkpoints(),
            exchangeRates,
            baseCurrency,
            useLatestPricesAtEnd);
    return portfolioMapper.toValuationResponse(
        baseCurrency, points, valuationExchangeRates(snapshots, exchangeRates, baseCurrency));
  }

  public List<String> symbolsForVariations(List<AssetVariationEntity> variations) {
    LinkedHashSet<String> symbols = new LinkedHashSet<>();
    variations.stream()
        .map(AssetVariationEntity::getSymbol)
        .map(PortfolioValuationHelper::normalizeSymbol)
        .forEach(symbols::add);
    return List.copyOf(symbols);
  }

  public List<String> symbolsForPositions(List<PortfolioValuationPosition> positions) {
    return positions.stream()
        .map(PortfolioValuationPosition::symbol)
        .map(PortfolioValuationHelper::normalizeSymbol)
        .distinct()
        .toList();
  }

  public List<Instant> valuationTimestamps(
      Instant from,
      Instant to,
      List<AssetVariationEntity> rangeVariations,
      List<PriceCheckpoint> sampledHistory) {
    Set<Instant> timestamps = new TreeSet<>();
    timestamps.add(from);
    timestamps.add(to);
    rangeVariations.stream().map(portfolioMapper::toInstant).forEach(timestamps::add);
    sampledHistory.stream()
        .map(PriceCheckpoint::capturedAt)
        .filter(timestamp -> !timestamp.isBefore(from) && !timestamp.isAfter(to))
        .forEach(timestamps::add);
    return List.copyOf(timestamps);
  }

  public List<PortfolioPositionSnapshot> reconstructPositions(
      List<AssetVariationEntity> initialStates,
      List<AssetVariationEntity> rangeVariations,
      List<Instant> timestamps) {
    if (timestamps.isEmpty()) {
      return List.of();
    }

    Map<Long, PortfolioValuationPosition> positionsByAssetId = new LinkedHashMap<>();
    initialStates.forEach(variation -> applyVariation(positionsByAssetId, variation));

    List<AssetVariationEntity> orderedVariations =
        rangeVariations.stream()
            .sorted(
                Comparator.comparing(AssetVariationEntity::getOccurredAt)
                    .thenComparing(
                        AssetVariationEntity::getId, Comparator.nullsLast(Long::compareTo)))
            .toList();
    List<Instant> orderedTimestamps = timestamps.stream().sorted().toList();
    List<PortfolioPositionSnapshot> snapshots = new ArrayList<>(orderedTimestamps.size());
    int variationIndex = 0;

    for (Instant timestamp : orderedTimestamps) {
      variationIndex =
          applyVariationsThrough(timestamp, orderedVariations, variationIndex, positionsByAssetId);
      snapshots.add(
          new PortfolioPositionSnapshot(timestamp, List.copyOf(positionsByAssetId.values())));
    }

    return List.copyOf(snapshots);
  }

  private int applyVariationsThrough(
      Instant timestamp,
      List<AssetVariationEntity> orderedVariations,
      int variationIndex,
      Map<Long, PortfolioValuationPosition> positionsByAssetId) {
    while (variationIndex < orderedVariations.size()
        && !portfolioMapper.toInstant(orderedVariations.get(variationIndex)).isAfter(timestamp)) {
      applyVariation(positionsByAssetId, orderedVariations.get(variationIndex));
      variationIndex++;
    }
    return variationIndex;
  }

  public List<PortfolioValuationPointDto> calculateValuationPoints(
      List<PortfolioPositionSnapshot> snapshots,
      List<PriceCheckpoint> priceHistory,
      Map<String, LatestMarketPrice> latestPrices,
      Map<String, PriceCheckpoint> endCheckpoints,
      Map<String, ExchangeRate> exchangeRates,
      CurrencyCodeEnum baseCurrency,
      boolean useLatestPricesAtEnd) {
    List<PriceCheckpoint> orderedHistory =
        priceHistory.stream().sorted(Comparator.comparing(PriceCheckpoint::capturedAt)).toList();
    Map<String, PriceCheckpoint> pricesBySymbol = new HashMap<>();
    List<PortfolioValuationPointDto> points = new ArrayList<>(snapshots.size());
    int checkpointIndex = 0;

    for (int snapshotIndex = 0; snapshotIndex < snapshots.size(); snapshotIndex++) {
      PortfolioPositionSnapshot snapshot = snapshots.get(snapshotIndex);
      while (checkpointIndex < orderedHistory.size()
          && !orderedHistory.get(checkpointIndex).capturedAt().isAfter(snapshot.timestamp())) {
        PriceCheckpoint checkpoint = orderedHistory.get(checkpointIndex++);
        pricesBySymbol.put(normalizeSymbol(checkpoint.symbol()), checkpoint);
      }

      boolean isEndPoint = snapshotIndex == snapshots.size() - 1;
      BigDecimal unrealizedProfitLoss = BigDecimal.ZERO;
      BigDecimal acquisitionCost = BigDecimal.ZERO;
      List<PortfolioValuationPriceObservationDto> priceObservations =
          new ArrayList<>(snapshot.positions().size());
      for (PortfolioValuationPosition position : snapshot.positions()) {
        String symbol = normalizeSymbol(position.symbol());
        MarketPriceObservation marketPrice =
            marketPriceAt(
                symbol,
                isEndPoint && useLatestPricesAtEnd,
                isEndPoint,
                latestPrices,
                endCheckpoints,
                pricesBySymbol);
        priceObservations.add(
            new PortfolioValuationPriceObservationDto(
                symbol, marketPrice.source(), marketPrice.capturedAt()));
        BigDecimal marketPriceRate =
            exchangeRate(
                CurrencyCodeEnum.EUR.getAbbreviation(),
                baseCurrency.getAbbreviation(),
                exchangeRates,
                symbol);
        BigDecimal costRate =
            exchangeRate(
                position.currency().getAbbreviation(),
                baseCurrency.getAbbreviation(),
                exchangeRates,
                symbol);

        BigDecimal convertedMarketPrice =
            marketPrice.price().multiply(marketPriceRate, MathContext.DECIMAL128);
        BigDecimal convertedAveragePrice =
            position.averagePrice().multiply(costRate, MathContext.DECIMAL128);
        unrealizedProfitLoss =
            unrealizedProfitLoss.add(
                position
                    .quantity()
                    .multiply(
                        convertedMarketPrice.subtract(convertedAveragePrice),
                        MathContext.DECIMAL128));
        acquisitionCost =
            acquisitionCost.add(
                position.quantity().multiply(convertedAveragePrice, MathContext.DECIMAL128));
      }
      points.add(
          portfolioMapper.toValuationPoint(
              snapshot.timestamp(), unrealizedProfitLoss, acquisitionCost, priceObservations));
    }
    return List.copyOf(points);
  }

  public Set<String> requiredCurrencyPairs(
      List<PortfolioPositionSnapshot> snapshots, CurrencyCodeEnum baseCurrency) {
    if (snapshots.stream().noneMatch(snapshot -> !snapshot.positions().isEmpty())) {
      return Set.of();
    }
    LinkedHashSet<String> pairs = new LinkedHashSet<>();
    addCurrencyPair(pairs, CurrencyCodeEnum.EUR.getAbbreviation(), baseCurrency.getAbbreviation());
    snapshots.stream()
        .flatMap(snapshot -> snapshot.positions().stream())
        .map(position -> position.currency().getAbbreviation())
        .forEach(currency -> addCurrencyPair(pairs, currency, baseCurrency.getAbbreviation()));
    return Set.copyOf(pairs);
  }

  public static String exchangeRateKey(String fromCurrency, String toCurrency) {
    return fromCurrency + "->" + toCurrency;
  }

  private static void applyVariation(
      Map<Long, PortfolioValuationPosition> positionsByAssetId, AssetVariationEntity variation) {
    Long assetId = variation.getAsset().getId();
    if (variation.getChangeType() == AssetVariationTypeEnum.DELETED) {
      positionsByAssetId.remove(assetId);
      return;
    }

    positionsByAssetId.put(
        assetId,
        new PortfolioValuationPosition(
            assetId,
            variation.getSymbol(),
            variation.getCurrency(),
            variation.getNewQuantity(),
            variation.getNewAveragePrice()));
  }

  private static MarketPriceObservation marketPriceAt(
      String symbol,
      boolean useLatestPrice,
      boolean useEndCheckpoint,
      Map<String, LatestMarketPrice> latestPrices,
      Map<String, PriceCheckpoint> endCheckpoints,
      Map<String, PriceCheckpoint> historicalPrices) {
    if (useLatestPrice) {
      LatestMarketPrice latestPrice = latestPrices.get(symbol);
      if (latestPrice != null) {
        return new MarketPriceObservation(
            latestPrice.price(), latestPrice.capturedAt(), PortfolioValuationPriceSource.LATEST);
      }
    }
    PriceCheckpoint checkpoint = useEndCheckpoint ? endCheckpoints.get(symbol) : null;
    if (checkpoint == null) {
      checkpoint = historicalPrices.get(symbol);
    }
    if (checkpoint == null) {
      throw new PortfolioException(
          PORTFOLIO_VALUATION_MARKET_PRICE_MISSING,
          Map.of(NAME, symbol),
          HttpStatus.SERVICE_UNAVAILABLE);
    }
    return new MarketPriceObservation(
        checkpoint.price(), checkpoint.capturedAt(), PortfolioValuationPriceSource.CHECKPOINT);
  }

  private List<PortfolioValuationExchangeRateDto> valuationExchangeRates(
      List<PortfolioPositionSnapshot> snapshots,
      Map<String, ExchangeRate> exchangeRates,
      CurrencyCodeEnum baseCurrency) {
    if (snapshots.stream().noneMatch(snapshot -> !snapshot.positions().isEmpty())) {
      return List.of();
    }
    return requiredCurrencyPairs(snapshots, baseCurrency).stream()
        .sorted()
        .map(exchangeRates::get)
        .filter(java.util.Objects::nonNull)
        .map(
            rate ->
                new PortfolioValuationExchangeRateDto(
                    rate.baseCurrency(), rate.quoteCurrency(), rate.effectiveDate()))
        .toList();
  }

  private record MarketPriceObservation(
      BigDecimal price, Instant capturedAt, PortfolioValuationPriceSource source) {}

  private static BigDecimal exchangeRate(
      String fromCurrency,
      String toCurrency,
      Map<String, ExchangeRate> exchangeRates,
      String symbol) {
    if (fromCurrency.equals(toCurrency)) {
      return BigDecimal.ONE;
    }
    ExchangeRate exchangeRate = exchangeRates.get(exchangeRateKey(fromCurrency, toCurrency));
    if (exchangeRate == null) {
      throw new PortfolioException(
          PORTFOLIO_VALUATION_EXCHANGE_RATE_MISSING,
          Map.of(ERROR, fromCurrency + "->" + toCurrency + " for " + symbol),
          HttpStatus.SERVICE_UNAVAILABLE);
    }
    return exchangeRate.rate();
  }

  private static void addCurrencyPair(Set<String> pairs, String fromCurrency, String toCurrency) {
    if (!fromCurrency.equals(toCurrency)) {
      pairs.add(exchangeRateKey(fromCurrency, toCurrency));
    }
  }

  private static String normalizeSymbol(String symbol) {
    return symbol.trim().toUpperCase(java.util.Locale.ROOT);
  }
}

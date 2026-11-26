package com.tychewealth.service.helper.marketdata;

import com.tychewealth.marketdataclient.MarketDataClient;
import com.tychewealth.marketdataclient.dto.ExchangeRate;
import com.tychewealth.marketdataclient.dto.LatestMarketPrice;
import com.tychewealth.marketdataclient.dto.MarketDataPage;
import com.tychewealth.marketdataclient.dto.PriceCheckpoint;
import com.tychewealth.model.PortfolioValuationEndPrices;
import com.tychewealth.service.helper.portfolio.PortfolioValuationHelper;
import com.tychewealth.serviceauth.client.ServiceAccessTokenProvider;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Retrieves and adapts the market data required by portfolio valuation. */
@Component
public class PortfolioValuationMarketDataHelper {

  private static final String MARKET_DATA_SERVICE = "market-data-service";
  private static final String PORTFOLIO_SERVICE = "portfolio-service";

  private final MarketDataClient marketDataClient;
  private final ServiceAccessTokenProvider serviceAccessTokenProvider;

  public PortfolioValuationMarketDataHelper(
      MarketDataClient marketDataClient, ServiceAccessTokenProvider serviceAccessTokenProvider) {
    this.marketDataClient = marketDataClient;
    this.serviceAccessTokenProvider = serviceAccessTokenProvider;
  }

  public List<PriceCheckpoint> retrievePriceHistory(
      List<String> symbols, Instant from, Instant to) {
    if (symbols.isEmpty()) {
      return List.of();
    }
    String accessToken =
        serviceAccessTokenProvider.getAccessToken(MARKET_DATA_SERVICE, PORTFOLIO_SERVICE);
    List<PriceCheckpoint> history =
        new ArrayList<>(marketDataClient.retrievePriceCheckpoints(symbols, from, accessToken));
    history.addAll(marketDataClient.getSampledPriceHistory(symbols, from, to, accessToken));
    return List.copyOf(history);
  }

  public PortfolioValuationEndPrices retrieveEndPrices(
      List<String> symbols, Instant to, boolean useLatestPrices) {
    if (symbols.isEmpty()) {
      return new PortfolioValuationEndPrices(Map.of(), Map.of());
    }
    String accessToken =
        serviceAccessTokenProvider.getAccessToken(MARKET_DATA_SERVICE, PORTFOLIO_SERVICE);
    if (!useLatestPrices) {
      return new PortfolioValuationEndPrices(
          Map.of(),
          indexCheckpoints(marketDataClient.retrievePriceCheckpoints(symbols, to, accessToken)));
    }

    MarketDataPage<LatestMarketPrice> latestPage =
        marketDataClient.getLatestPrices(symbols, 0, symbols.size(), accessToken);
    Map<String, LatestMarketPrice> latestPrices = indexLatestPrices(latestPage.content());
    List<String> missingSymbols =
        symbols.stream().filter(symbol -> !latestPrices.containsKey(symbol)).toList();
    Map<String, PriceCheckpoint> checkpoints =
        missingSymbols.isEmpty()
            ? Map.of()
            : indexCheckpoints(
                marketDataClient.retrievePriceCheckpoints(missingSymbols, to, accessToken));
    return new PortfolioValuationEndPrices(latestPrices, checkpoints);
  }

  public Map<String, ExchangeRate> retrieveExchangeRates(Set<String> requiredPairs) {
    if (requiredPairs.isEmpty()) {
      return Map.of();
    }

    List<String> baseCurrencies = new ArrayList<>(requiredPairs.size());
    List<String> quoteCurrencies = new ArrayList<>(requiredPairs.size());
    for (String pair : requiredPairs) {
      String[] currencies = pair.split("->", 2);
      baseCurrencies.add(currencies[0]);
      quoteCurrencies.add(currencies[1]);
    }
    String accessToken =
        serviceAccessTokenProvider.getAccessToken(MARKET_DATA_SERVICE, PORTFOLIO_SERVICE);
    List<ExchangeRate> rates =
        marketDataClient.getExchangeRates(baseCurrencies, quoteCurrencies, accessToken);
    Map<String, ExchangeRate> ratesByPair = new LinkedHashMap<>();
    if (rates != null) {
      for (ExchangeRate rate : rates) {
        ratesByPair.put(
            PortfolioValuationHelper.exchangeRateKey(rate.baseCurrency(), rate.quoteCurrency()),
            rate);
      }
    }
    return Map.copyOf(ratesByPair);
  }

  private static Map<String, LatestMarketPrice> indexLatestPrices(List<LatestMarketPrice> prices) {
    Map<String, LatestMarketPrice> pricesBySymbol = new LinkedHashMap<>();
    for (LatestMarketPrice price : prices) {
      pricesBySymbol.put(PortfolioMarketDataHelper.normalizeSymbol(price.symbol()), price);
    }
    return Map.copyOf(pricesBySymbol);
  }

  private static Map<String, PriceCheckpoint> indexCheckpoints(List<PriceCheckpoint> checkpoints) {
    Map<String, PriceCheckpoint> checkpointsBySymbol = new LinkedHashMap<>();
    for (PriceCheckpoint checkpoint : checkpoints) {
      checkpointsBySymbol.put(
          PortfolioMarketDataHelper.normalizeSymbol(checkpoint.symbol()), checkpoint);
    }
    return Map.copyOf(checkpointsBySymbol);
  }
}

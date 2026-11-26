package com.tychewealth.service.impl;

import com.tychewealth.dto.marketprice.LatestPriceResponseDto;
import com.tychewealth.dto.marketprice.PriceCheckpointDto;
import com.tychewealth.entity.MarketPriceCheckpointEntity;
import com.tychewealth.error.ErrorDefinition;
import com.tychewealth.error.exception.InvalidMarketPriceQueryException;
import com.tychewealth.listing.filter.ListQuery;
import com.tychewealth.mapper.MarketPriceReadMapper;
import com.tychewealth.model.HistoryResolution;
import com.tychewealth.service.LatestPricesResult;
import com.tychewealth.service.MarketPriceCheckpointService;
import com.tychewealth.service.MarketPriceReadService;
import com.tychewealth.service.helper.MarketPriceHistorySampler;
import com.tychewealth.service.helper.MarketPriceReadHelper;
import com.tychewealth.store.MarketQuoteStore;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

/** Implements reads from the latest-quote cache and persistent price checkpoints. */
@Service
@RequiredArgsConstructor
public class MarketPriceReadServiceImpl implements MarketPriceReadService {

  private final MarketQuoteStore marketQuoteStore;
  private final MarketPriceCheckpointService marketPriceCheckpointService;
  private final MarketPriceReadHelper readHelper;
  private final MarketPriceReadMapper readMapper;
  private final MarketPriceHistorySampler historySampler;

  @Override
  public LatestPricesResult getLatestPrices(ListQuery query) {
    Page<String> symbols = readHelper.pageLatestSymbols(query);
    List<LatestPriceResponseDto> prices =
        marketQuoteStore.findBySymbols(symbols.getContent()).stream()
            .map(readMapper::toLatestPriceResponse)
            .toList();
    return new LatestPricesResult(prices, symbols.getTotalElements());
  }

  @Override
  public Page<PriceCheckpointDto> getPriceHistory(ListQuery query) {
    readHelper.validateHistoryFilters(query.filter());
    Page<MarketPriceCheckpointEntity> page =
        marketPriceCheckpointService.findCheckpoints(
            readHelper.specification(query), readHelper.pageRequest(query.pageCriteria()));
    return page.map(readMapper::toPriceCheckpointDto);
  }

  @Override
  public Page<PriceCheckpointDto> getPriceHistory(ListQuery query, HistoryResolution resolution) {
    readHelper.validateHistoryFilters(query.filter());
    List<PriceCheckpointDto> sampled =
        historySampler.sample(readHelper.specification(query), resolution).stream()
            .map(readMapper::toPriceCheckpointDto)
            .toList();
    var pageable = readHelper.pageRequest(query.pageCriteria());
    int start = (int) Math.min(pageable.getOffset(), sampled.size());
    int end = Math.min(start + pageable.getPageSize(), sampled.size());
    return new PageImpl<>(sampled.subList(start, end), pageable, sampled.size());
  }

  @Override
  public List<PriceCheckpointDto> getSampledPriceHistory(
      ListQuery query, Instant from, Instant to) {
    readHelper.validateHistoryFilters(query.filter());
    return historySampler
        .sample(readHelper.specification(query), historySampler.resolution(from, to))
        .stream()
        .map(readMapper::toPriceCheckpointDto)
        .toList();
  }

  @Override
  public List<PriceCheckpointDto> retrieveLatestPriceCheckpoints(
      List<String> symbols, Instant requestedCapturedAt) {
    List<String> normalizedSymbols = readHelper.normalizeSymbols(symbols);
    Instant capturedAt = requestedCapturedAt == null ? Instant.now() : requestedCapturedAt;
    LocalDateTime capturedAtUtc;
    try {
      capturedAtUtc = LocalDateTime.ofInstant(capturedAt, ZoneOffset.UTC);
    } catch (DateTimeException exception) {
      throw new InvalidMarketPriceQueryException(ErrorDefinition.MARKET_PRICE_CAPTURED_AT_INVALID);
    }

    return marketPriceCheckpointService
        .findLatestBySymbolsAtOrBefore(normalizedSymbols, capturedAtUtc)
        .stream()
        .map(readMapper::toPriceCheckpointDto)
        .toList();
  }
}

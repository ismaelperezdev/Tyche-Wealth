package com.tychewealth.service.impl;

import com.tychewealth.dto.marketprice.LatestPriceResponseDto;
import com.tychewealth.dto.marketprice.PriceCheckpointDto;
import com.tychewealth.entity.MarketPriceCheckpointEntity;
import com.tychewealth.listing.filter.ListQuery;
import com.tychewealth.mapper.MarketPriceReadMapper;
import com.tychewealth.service.LatestPricesResult;
import com.tychewealth.service.MarketPriceCheckpointService;
import com.tychewealth.service.MarketPriceReadService;
import com.tychewealth.service.helper.MarketPriceReadHelper;
import com.tychewealth.store.MarketQuoteStore;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implements reads from the latest-quote cache and persistent price checkpoints. */
@Service
@RequiredArgsConstructor
public class MarketPriceReadServiceImpl implements MarketPriceReadService {

  private final MarketQuoteStore marketQuoteStore;
  private final MarketPriceCheckpointService marketPriceCheckpointService;
  private final MarketPriceReadHelper readHelper;
  private final MarketPriceReadMapper readMapper;

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
  @Transactional(readOnly = true)
  public Page<PriceCheckpointDto> getPriceHistory(ListQuery query) {
    readHelper.validateHistoryFilters(query.filter());
    Page<MarketPriceCheckpointEntity> page =
        marketPriceCheckpointService.findCheckpoints(
            readHelper.specification(query), readHelper.pageRequest(query.pageCriteria()));
    return page.map(readMapper::toPriceCheckpointDto);
  }
}

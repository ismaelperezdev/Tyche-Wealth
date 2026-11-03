package com.tychewealth.service;

import com.tychewealth.dto.marketprice.PriceCheckpointDto;
import com.tychewealth.listing.filter.ListQuery;
import org.springframework.data.domain.Page;

/** Reads latest quotes and persisted checkpoints for the internal market-price API. */
public interface MarketPriceReadService {

  LatestPricesResult getLatestPrices(ListQuery query);

  Page<PriceCheckpointDto> getPriceHistory(ListQuery query);
}

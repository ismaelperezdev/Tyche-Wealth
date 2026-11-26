package com.tychewealth.service;

import com.tychewealth.dto.marketprice.PriceCheckpointDto;
import com.tychewealth.listing.filter.ListQuery;
import com.tychewealth.model.HistoryResolution;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;

/** Reads latest quotes and persisted checkpoints for the internal market-price API. */
public interface MarketPriceReadService {

  LatestPricesResult getLatestPrices(ListQuery query);

  Page<PriceCheckpointDto> getPriceHistory(ListQuery query);

  Page<PriceCheckpointDto> getPriceHistory(ListQuery query, HistoryResolution resolution);

  List<PriceCheckpointDto> getSampledPriceHistory(ListQuery query, Instant from, Instant to);

  List<PriceCheckpointDto> retrieveLatestPriceCheckpoints(List<String> symbols, Instant capturedAt);
}

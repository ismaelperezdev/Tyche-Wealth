package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.LATEST_PRICES_URL;
import static com.tychewealth.constants.ApiConstants.PRICE_HISTORY_URL;
import static com.tychewealth.constants.ApiConstants.REQUEST_PRODUCES;
import static com.tychewealth.constants.ApiConstants.SAMPLED_PRICE_HISTORY_URL;

import com.tychewealth.dto.marketprice.LatestPriceResponseDto;
import com.tychewealth.dto.marketprice.PriceCheckpointDto;
import com.tychewealth.listing.Listable;
import com.tychewealth.listing.filter.ListQuery;
import com.tychewealth.service.helper.MarketPriceLatestFilterFieldMapping;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Defines the HTTP contract for reading current and historical market prices. */
@RequestMapping
@Tag(name = "Market Price")
public interface MarketPriceApi {

  /** Returns paginated latest prices for the requested symbols. */
  @Listable(mapping = MarketPriceLatestFilterFieldMapping.class)
  @GetMapping(value = LATEST_PRICES_URL, produces = REQUEST_PRODUCES)
  ResponseEntity<List<LatestPriceResponseDto>> getLatestPrices(
      ListQuery query, HttpServletRequest request);

  /** Retrieves the latest persisted checkpoint per symbol at or before the requested instant. */
  @GetMapping(value = PRICE_HISTORY_URL, produces = REQUEST_PRODUCES)
  ResponseEntity<List<PriceCheckpointDto>> retrievePriceCheckpoints(
      @Parameter(name = "symbol.in", required = true)
          @RequestParam(name = "symbol.in", required = false)
          List<String> symbols,
      @RequestParam(name = "capturedAt", required = false) String capturedAt);

  /** Returns the complete sampled history for chart consumers without pagination. */
  @GetMapping(value = SAMPLED_PRICE_HISTORY_URL, produces = REQUEST_PRODUCES)
  ResponseEntity<List<PriceCheckpointDto>> getSampledPriceHistory(
      @Parameter(name = "symbol.in", required = true)
          @RequestParam(name = "symbol.in", required = false)
          List<String> symbols,
      @RequestParam(name = "capturedAt.gte") Instant from,
      @RequestParam(name = "capturedAt.lte", required = false) Instant to);
}

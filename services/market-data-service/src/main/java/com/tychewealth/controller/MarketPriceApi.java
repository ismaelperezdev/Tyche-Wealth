package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.LATEST_PRICES_URL;
import static com.tychewealth.constants.ApiConstants.PRICE_HISTORY_URL;
import static com.tychewealth.constants.ApiConstants.REQUEST_PRODUCES;

import com.tychewealth.dto.marketprice.LatestPriceResponseDto;
import com.tychewealth.dto.marketprice.PriceCheckpointDto;
import com.tychewealth.listing.Listable;
import com.tychewealth.listing.filter.ListQuery;
import com.tychewealth.service.helper.MarketPriceFilterFieldMapping;
import com.tychewealth.service.helper.MarketPriceLatestFilterFieldMapping;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Defines the HTTP contract for reading current and historical market prices. */
@RequestMapping
@Tag(name = "Market Price")
public interface MarketPriceApi {

  /** Returns paginated latest prices for the requested symbols. */
  @Listable(mapping = MarketPriceLatestFilterFieldMapping.class)
  @GetMapping(value = LATEST_PRICES_URL, produces = REQUEST_PRODUCES)
  ResponseEntity<List<LatestPriceResponseDto>> getLatestPrices(
      ListQuery query, HttpServletRequest request);

  /** Returns persisted six-hour checkpoints within an inclusive UTC time range. */
  @Listable(mapping = MarketPriceFilterFieldMapping.class)
  @GetMapping(value = PRICE_HISTORY_URL, produces = REQUEST_PRODUCES)
  ResponseEntity<List<PriceCheckpointDto>> getPriceHistory(
      ListQuery query, HttpServletRequest request);
}

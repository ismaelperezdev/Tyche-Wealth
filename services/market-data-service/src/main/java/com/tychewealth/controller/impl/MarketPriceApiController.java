package com.tychewealth.controller.impl;

import com.tychewealth.controller.MarketPriceApi;
import com.tychewealth.dto.marketprice.LatestPriceResponseDto;
import com.tychewealth.dto.marketprice.PriceCheckpointDto;
import com.tychewealth.listing.domain.ApiFilter;
import com.tychewealth.listing.domain.PageCriteria;
import com.tychewealth.listing.enums.FilterOperator;
import com.tychewealth.listing.filter.ListQuery;
import com.tychewealth.listing.filter.PaginationHttpHeaders;
import com.tychewealth.service.LatestPricesResult;
import com.tychewealth.service.MarketPriceReadService;
import com.tychewealth.service.helper.MarketPriceQueryValidationHelper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** HTTP entry point for internal market-price reads. */
@RestController
@RequiredArgsConstructor
public class MarketPriceApiController implements MarketPriceApi {

  private final MarketPriceReadService marketPriceReadService;
  private final MarketPriceQueryValidationHelper validationHelper;

  @Override
  public ResponseEntity<List<LatestPriceResponseDto>> getLatestPrices(
      ListQuery query, HttpServletRequest request) {
    LatestPricesResult result = marketPriceReadService.getLatestPrices(query);
    HttpHeaders paginationHeaders =
        PaginationHttpHeaders.build(
            request.getRequestURI(),
            query.pageCriteria().offset(),
            query.pageCriteria().limit(),
            result.totalCandidates(),
            request.getQueryString());
    return ResponseEntity.ok().headers(paginationHeaders).body(result.content());
  }

  @Override
  public ResponseEntity<List<PriceCheckpointDto>> retrievePriceCheckpoints(
      List<String> symbols, List<String> capturedAts) {
    if (capturedAts != null && capturedAts.size() > 1) {
      return ResponseEntity.ok(
          marketPriceReadService.retrieveLatestPriceCheckpoints(
              symbols, capturedAts.stream().map(validationHelper::parseCapturedAt).toList()));
    }
    return ResponseEntity.ok(
        marketPriceReadService.retrieveLatestPriceCheckpoints(
            symbols,
            validationHelper.parseCapturedAt(
                capturedAts == null || capturedAts.isEmpty() ? null : capturedAts.getFirst())));
  }

  @Override
  public ResponseEntity<List<PriceCheckpointDto>> getSampledPriceHistory(
      List<String> symbols, String from, String to) {
    Instant parsedFrom = validationHelper.parseCapturedAt(from);
    Instant parsedTo = validationHelper.parseCapturedAt(to);
    validationHelper.validateSampledHistoryRange(parsedFrom, parsedTo);
    List<ApiFilter> filters = new ArrayList<>();
    if (symbols != null) {
      filters.add(new ApiFilter("symbol", FilterOperator.IN, symbols.toArray(String[]::new)));
    }
    if (parsedFrom != null) {
      filters.add(new ApiFilter("capturedAt", FilterOperator.GTE, parsedFrom.toString()));
    }
    if (parsedTo != null) {
      filters.add(new ApiFilter("capturedAt", FilterOperator.LTE, parsedTo.toString()));
    }
    ListQuery query = new ListQuery(filters, new PageCriteria(0, Integer.MAX_VALUE, List.of()));
    return ResponseEntity.ok(
        marketPriceReadService.getSampledPriceHistory(query, parsedFrom, parsedTo));
  }
}

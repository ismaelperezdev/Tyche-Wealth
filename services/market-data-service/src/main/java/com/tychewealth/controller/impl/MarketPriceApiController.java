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
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** HTTP entry point for internal market-price reads. */
@RestController
@RequiredArgsConstructor
public class MarketPriceApiController implements MarketPriceApi {

  private final MarketPriceReadService marketPriceReadService;

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
  public ResponseEntity<List<PriceCheckpointDto>> getPriceHistory(
      ListQuery query, HttpServletRequest request) {
    Page<PriceCheckpointDto> page = marketPriceReadService.getPriceHistory(query);
    HttpHeaders paginationHeaders =
        PaginationHttpHeaders.build(
            request.getRequestURI(),
            query.pageCriteria().offset(),
            query.pageCriteria().limit(),
            page.getTotalElements(),
            request.getQueryString());
    return ResponseEntity.ok().headers(paginationHeaders).body(page.getContent());
  }

  @Override
  public ResponseEntity<List<PriceCheckpointDto>> getSampledPriceHistory(
      List<String> symbols, Instant from, Instant to) {
    List<ApiFilter> filters = new ArrayList<>();
    filters.add(new ApiFilter("symbol", FilterOperator.IN, symbols.toArray(String[]::new)));
    if (from != null) {
      filters.add(new ApiFilter("capturedAt", FilterOperator.GTE, from.toString()));
    }
    if (to != null) {
      filters.add(new ApiFilter("capturedAt", FilterOperator.LTE, to.toString()));
    }
    ListQuery query = new ListQuery(filters, new PageCriteria(0, Integer.MAX_VALUE, List.of()));
    return ResponseEntity.ok(marketPriceReadService.getSampledPriceHistory(query, from, to));
  }
}

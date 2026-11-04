package com.tychewealth.controller.impl;

import com.tychewealth.controller.MarketPriceApi;
import com.tychewealth.dto.marketprice.LatestPriceResponseDto;
import com.tychewealth.dto.marketprice.PriceCheckpointDto;
import com.tychewealth.listing.filter.ListQuery;
import com.tychewealth.listing.filter.PaginationHttpHeaders;
import com.tychewealth.service.LatestPricesResult;
import com.tychewealth.service.MarketPriceReadService;
import jakarta.servlet.http.HttpServletRequest;
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
}

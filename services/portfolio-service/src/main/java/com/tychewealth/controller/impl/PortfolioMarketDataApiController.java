package com.tychewealth.controller.impl;

import static com.tychewealth.constants.SecurityConstants.CACHE_CONTROL_NO_STORE_HEADER_VALUE;
import static com.tychewealth.constants.SecurityConstants.PRAGMA_NO_CACHE_HEADER_VALUE;

import com.tychewealth.controller.PortfolioMarketDataApi;
import com.tychewealth.dto.marketdata.AssetMarketPriceDto;
import com.tychewealth.dto.marketdata.AssetPriceCheckpointDto;
import com.tychewealth.ratelimit.RateLimitKey;
import com.tychewealth.ratelimit.RateLimited;
import com.tychewealth.service.PortfolioMarketDataService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** HTTP entry point for market data associated with the authenticated user's assets. */
@RestController
@RequiredArgsConstructor
public class PortfolioMarketDataApiController implements PortfolioMarketDataApi {

  private final PortfolioMarketDataService portfolioMarketDataService;

  @Override
  @RateLimited(RateLimitKey.PORTFOLIO_MARKET_DATA_LATEST)
  public ResponseEntity<List<AssetMarketPriceDto>> getLatestPrices(
      @AuthenticationPrincipal Long userId,
      @RequestParam(name = "portfolioIds", required = false) List<Long> portfolioIds,
      @RequestParam(name = "assetIds", required = false) List<Long> assetIds,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
    List<AssetMarketPriceDto> response =
        portfolioMarketDataService.getLatestPrices(
            userId, portfolioIds, assetIds, authorizationHeader);
    return noStoreResponse(response);
  }

  @Override
  @RateLimited(RateLimitKey.PORTFOLIO_MARKET_DATA_HISTORY)
  public ResponseEntity<List<AssetPriceCheckpointDto>> getPriceHistory(
      @AuthenticationPrincipal Long userId,
      @RequestParam(name = "portfolioIds", required = false) List<Long> portfolioIds,
      @RequestParam(name = "assetIds", required = false) List<Long> assetIds,
      @RequestParam(name = "from", required = false) Instant from,
      @RequestParam(name = "to", required = false) Instant to,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
    List<AssetPriceCheckpointDto> response =
        portfolioMarketDataService.getPriceHistory(
            userId, portfolioIds, assetIds, from, to, authorizationHeader);
    return noStoreResponse(response);
  }

  private static <T> ResponseEntity<T> noStoreResponse(T body) {
    return ResponseEntity.status(HttpStatus.OK)
        .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE)
        .header(HttpHeaders.PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE)
        .body(body);
  }
}

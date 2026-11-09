package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.PORTFOLIO_BASE_URL;
import static com.tychewealth.constants.ApiConstants.REQUEST_PRODUCES;

import com.tychewealth.dto.marketdata.AssetMarketPriceDto;
import com.tychewealth.dto.marketdata.AssetPriceCheckpointDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Defines market-data reads for assets owned by the authenticated user. */
@RequestMapping(value = PORTFOLIO_BASE_URL + "/market-data")
@Tag(name = "Portfolio Market Data")
public interface PortfolioMarketDataApi {

  /** Returns the latest market price for each selected active asset. */
  @GetMapping(value = "/latest", produces = REQUEST_PRODUCES)
  ResponseEntity<List<AssetMarketPriceDto>> getLatestPrices(
      @AuthenticationPrincipal Long userId,
      @RequestParam(name = "portfolioIds", required = false) List<Long> portfolioIds,
      @RequestParam(name = "assetIds", required = false) List<Long> assetIds,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader);

  /** Returns market-price checkpoints for each selected active asset in the requested range. */
  @GetMapping(value = "/history", produces = REQUEST_PRODUCES)
  ResponseEntity<List<AssetPriceCheckpointDto>> getPriceHistory(
      @AuthenticationPrincipal Long userId,
      @RequestParam(name = "portfolioIds", required = false) List<Long> portfolioIds,
      @RequestParam(name = "assetIds", required = false) List<Long> assetIds,
      @RequestParam(name = "from", required = false) Instant from,
      @RequestParam(name = "to", required = false) Instant to,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader);
}

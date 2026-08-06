package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.DEFAULT_LIST_LIMIT;
import static com.tychewealth.constants.ApiConstants.DEFAULT_PAGE;
import static com.tychewealth.constants.ApiConstants.LIMIT_PARAM;
import static com.tychewealth.constants.ApiConstants.PAGE_PARAM;
import static com.tychewealth.constants.ApiConstants.PORTFOLIO_BASE_URL;
import static com.tychewealth.constants.ApiConstants.REQUEST_CONSUMES;
import static com.tychewealth.constants.ApiConstants.REQUEST_PRODUCES;

import com.tychewealth.dto.portfolio.PortfolioResponseDto;
import com.tychewealth.dto.portfolio.request.PortfolioCreateRequestDto;
import com.tychewealth.dto.portfolio.request.PortfolioUpdateRequestDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Defines the HTTP contract for user portfolio management.
 *
 * <p>Declares endpoints for listing, retrieving, creating, updating, and deleting portfolios owned
 * by the authenticated user.
 */
@RequestMapping(value = PORTFOLIO_BASE_URL)
@Tag(name = "Portfolio")
public interface PortfolioApi {

  /** Returns a paginated list of portfolios owned by the authenticated user. */
  @GetMapping(value = "/me", produces = REQUEST_PRODUCES)
  ResponseEntity<List<PortfolioResponseDto>> listPortfolios(
      @AuthenticationPrincipal Long userId,
      @RequestParam(name = PAGE_PARAM, defaultValue = DEFAULT_PAGE) int page,
      @RequestParam(name = LIMIT_PARAM, defaultValue = DEFAULT_LIST_LIMIT) int limit);

  /** Returns a portfolio owned by the authenticated user. */
  @GetMapping(value = "/me/{portfolioId}", produces = REQUEST_PRODUCES)
  ResponseEntity<PortfolioResponseDto> retrieve(
      @AuthenticationPrincipal Long userId, @PathVariable("portfolioId") Long portfolioId);

  /** Creates a portfolio for the authenticated user. */
  @PostMapping(consumes = REQUEST_CONSUMES, produces = REQUEST_PRODUCES)
  ResponseEntity<PortfolioResponseDto> create(
      @AuthenticationPrincipal Long userId,
      @Valid @RequestBody PortfolioCreateRequestDto createRequest);

  /** Updates a portfolio owned by the authenticated user. */
  @PatchMapping(
      value = "/me/{portfolioId}",
      consumes = REQUEST_CONSUMES,
      produces = REQUEST_PRODUCES)
  ResponseEntity<PortfolioResponseDto> update(
      @AuthenticationPrincipal Long userId,
      @PathVariable("portfolioId") Long portfolioId,
      @Valid @RequestBody PortfolioUpdateRequestDto updateRequest);

  /** Deletes a portfolio owned by the authenticated user. */
  @DeleteMapping(value = "/me/{portfolioId}")
  ResponseEntity<Void> delete(
      @AuthenticationPrincipal Long userId, @PathVariable("portfolioId") Long portfolioId);
}

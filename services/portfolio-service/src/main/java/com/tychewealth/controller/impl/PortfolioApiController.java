package com.tychewealth.controller.impl;

import static com.tychewealth.constants.LogConstants.CREATE_ACTION;
import static com.tychewealth.constants.LogConstants.DELETE_ACTION;
import static com.tychewealth.constants.LogConstants.LIST_PORTFOLIOS_ACTION;
import static com.tychewealth.constants.LogConstants.PORTFOLIO;
import static com.tychewealth.constants.LogConstants.PORTFOLIO_ID;
import static com.tychewealth.constants.LogConstants.REQUEST_START;
import static com.tychewealth.constants.LogConstants.REQUEST_SUCCESS;
import static com.tychewealth.constants.LogConstants.RETRIEVE_ACTION;
import static com.tychewealth.constants.LogConstants.UPDATE_ACTION;
import static com.tychewealth.constants.LogConstants.USER_ID;
import static com.tychewealth.utils.Utils.buildNoStoreBodyResponse;
import static com.tychewealth.utils.Utils.buildNoStoreEmptyResponse;

import com.tychewealth.controller.PortfolioApi;
import com.tychewealth.dto.portfolio.PortfolioResponseDto;
import com.tychewealth.dto.portfolio.request.PortfolioCreateRequestDto;
import com.tychewealth.dto.portfolio.request.PortfolioUpdateRequestDto;
import com.tychewealth.ratelimit.RateLimitKey;
import com.tychewealth.ratelimit.RateLimited;
import com.tychewealth.service.PortfolioService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@AllArgsConstructor
public class PortfolioApiController implements PortfolioApi {

  private final PortfolioService portfolioService;

  @Override
  @RateLimited(RateLimitKey.PORTFOLIO_LIST)
  public ResponseEntity<List<PortfolioResponseDto>> listPortfolios(
      @AuthenticationPrincipal Long userId) {
    log.info(REQUEST_START + USER_ID, PORTFOLIO, LIST_PORTFOLIOS_ACTION, userId);

    List<PortfolioResponseDto> response = portfolioService.listPortfolios(userId);
    log.info(REQUEST_SUCCESS + USER_ID, PORTFOLIO, LIST_PORTFOLIOS_ACTION, userId);

    return buildNoStoreBodyResponse(HttpStatus.OK, response);
  }

  @Override
  @RateLimited(RateLimitKey.PORTFOLIO_RETRIEVE)
  public ResponseEntity<PortfolioResponseDto> retrieve(
      @AuthenticationPrincipal Long userId, Long portfolioId) {
    log.info(
        REQUEST_START + PORTFOLIO_ID + USER_ID, PORTFOLIO, RETRIEVE_ACTION, portfolioId, userId);

    PortfolioResponseDto response = portfolioService.retrieve(userId, portfolioId);
    log.info(
        REQUEST_SUCCESS + PORTFOLIO_ID + USER_ID, PORTFOLIO, RETRIEVE_ACTION, portfolioId, userId);

    return buildNoStoreBodyResponse(HttpStatus.OK, response);
  }

  @Override
  @RateLimited(RateLimitKey.PORTFOLIO_CREATE)
  public ResponseEntity<PortfolioResponseDto> create(
      @AuthenticationPrincipal Long userId,
      @Valid @RequestBody PortfolioCreateRequestDto createRequest) {
    log.info(REQUEST_START + USER_ID, PORTFOLIO, CREATE_ACTION, userId);

    PortfolioResponseDto response = portfolioService.create(userId, createRequest);
    log.info(REQUEST_SUCCESS + USER_ID, PORTFOLIO, CREATE_ACTION, userId);

    return buildNoStoreBodyResponse(HttpStatus.CREATED, response);
  }

  @Override
  @RateLimited(RateLimitKey.PORTFOLIO_UPDATE)
  public ResponseEntity<PortfolioResponseDto> update(
      @AuthenticationPrincipal Long userId,
      Long portfolioId,
      @Valid @RequestBody PortfolioUpdateRequestDto updateRequest) {
    log.info(REQUEST_START + PORTFOLIO_ID + USER_ID, PORTFOLIO, UPDATE_ACTION, portfolioId, userId);

    PortfolioResponseDto response = portfolioService.update(userId, portfolioId, updateRequest);
    log.info(
        REQUEST_SUCCESS + PORTFOLIO_ID + USER_ID, PORTFOLIO, UPDATE_ACTION, portfolioId, userId);

    return buildNoStoreBodyResponse(HttpStatus.OK, response);
  }

  @Override
  @RateLimited(RateLimitKey.PORTFOLIO_DELETE)
  public ResponseEntity<Void> delete(@AuthenticationPrincipal Long userId, Long portfolioId) {
    log.info(REQUEST_START + PORTFOLIO_ID + USER_ID, PORTFOLIO, DELETE_ACTION, portfolioId, userId);

    portfolioService.delete(userId, portfolioId);
    log.info(
        REQUEST_SUCCESS + PORTFOLIO_ID + USER_ID, PORTFOLIO, DELETE_ACTION, portfolioId, userId);

    return buildNoStoreEmptyResponse(HttpStatus.NO_CONTENT);
  }
}

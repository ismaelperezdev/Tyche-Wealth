package com.tychewealth.controller.impl;

import static com.tychewealth.constants.LogConstants.CREATE_ACTION;
import static com.tychewealth.constants.LogConstants.PORTFOLIO;
import static com.tychewealth.constants.LogConstants.PORTFOLIO_NAME;
import static com.tychewealth.constants.LogConstants.REQUEST_START;
import static com.tychewealth.constants.LogConstants.REQUEST_SUCCESS;
import static com.tychewealth.constants.LogConstants.USER_ID;
import static org.springframework.http.ResponseEntity.status;

import com.tychewealth.controller.PortfolioApi;
import com.tychewealth.dto.portfolio.PortfolioResponseDto;
import com.tychewealth.dto.portfolio.request.PortfolioCreateRequestDto;
import com.tychewealth.service.PortfolioService;
import jakarta.validation.Valid;
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
  public ResponseEntity<PortfolioResponseDto> create(
      @AuthenticationPrincipal Long userId,
      @Valid @RequestBody PortfolioCreateRequestDto createRequest) {
    log.info(
        REQUEST_START + PORTFOLIO_NAME + USER_ID,
        PORTFOLIO,
        CREATE_ACTION,
        createRequest.getName(),
        userId);

    PortfolioResponseDto response = portfolioService.create(userId, createRequest);
    log.info(REQUEST_SUCCESS + USER_ID, PORTFOLIO, CREATE_ACTION, userId);

    return status(HttpStatus.CREATED).body(response);
  }
}

package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.PORTFOLIO_BASE_URL;
import static com.tychewealth.constants.ApiConstants.REQUEST_CONSUMES;
import static com.tychewealth.constants.ApiConstants.REQUEST_PRODUCES;

import com.tychewealth.dto.portfolio.PortfolioResponseDto;
import com.tychewealth.dto.portfolio.request.PortfolioCreateRequestDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(value = PORTFOLIO_BASE_URL)
@Tag(name = "Portfolio")
public interface PortfolioApi {

  @PostMapping(consumes = REQUEST_CONSUMES, produces = REQUEST_PRODUCES)
  ResponseEntity<PortfolioResponseDto> create(
      @AuthenticationPrincipal Long userId,
      @Valid @RequestBody PortfolioCreateRequestDto createRequest);
}

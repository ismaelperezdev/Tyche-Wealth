package com.tychewealth.controller.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.portfolio.PortfolioResponseDto;
import com.tychewealth.dto.portfolio.request.PortfolioCreateRequestDto;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.service.PortfolioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class PortfolioApiControllerTest {

  @Mock private PortfolioService portfolioService;

  @InjectMocks private PortfolioApiController portfolioApiController;

  @Test
  void createReturnsCreatedResponse() {
    PortfolioCreateRequestDto request = new PortfolioCreateRequestDto();
    request.setName("Core");
    request.setBaseCurrency(CurrencyCodeEnum.USD);

    PortfolioResponseDto response = new PortfolioResponseDto();
    response.setId(7L);
    response.setName("Core");

    when(portfolioService.create(42L, request)).thenReturn(response);

    ResponseEntity<PortfolioResponseDto> result = portfolioApiController.create(42L, request);

    assertEquals(HttpStatus.CREATED, result.getStatusCode());
    assertEquals(response, result.getBody());
    verify(portfolioService).create(42L, request);
  }
}

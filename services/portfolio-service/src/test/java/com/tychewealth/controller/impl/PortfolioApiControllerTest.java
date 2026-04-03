package com.tychewealth.controller.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.portfolio.PortfolioResponseDto;
import com.tychewealth.dto.portfolio.request.PortfolioCreateRequestDto;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.service.PortfolioService;
import java.util.List;
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
  void listPortfoliosReturnsOkResponse() {
    PortfolioResponseDto firstPortfolio = new PortfolioResponseDto();
    firstPortfolio.setId(7L);
    firstPortfolio.setName("Core");

    PortfolioResponseDto secondPortfolio = new PortfolioResponseDto();
    secondPortfolio.setId(8L);
    secondPortfolio.setName("Retirement");

    List<PortfolioResponseDto> response = List.of(firstPortfolio, secondPortfolio);

    when(portfolioService.listPortfolios(42L)).thenReturn(response);

    ResponseEntity<List<PortfolioResponseDto>> result = portfolioApiController.listPortfolios(42L);

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(response, result.getBody());
    verify(portfolioService).listPortfolios(42L);
  }

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

  @Test
  void deleteReturnsNoContentResponse() {
    ResponseEntity<Void> result = portfolioApiController.delete(42L, 7L);

    assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    verify(portfolioService).delete(42L, 7L);
  }
}

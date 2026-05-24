package com.tychewealth.controller.impl;

import static com.tychewealth.constants.SecurityConstants.CACHE_CONTROL_NO_STORE_HEADER_VALUE;
import static com.tychewealth.constants.SecurityConstants.PRAGMA_NO_CACHE_HEADER_VALUE;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_NAME_CORE;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_NAME_RETIREMENT;
import static com.tychewealth.constants.TestConstants.TEST_X_HAS_NEXT_HEADER;
import static com.tychewealth.constants.TestConstants.TEST_X_LIMIT_HEADER;
import static com.tychewealth.constants.TestConstants.TEST_X_PAGE_HEADER;
import static com.tychewealth.constants.TestConstants.TEST_X_TOTAL_COUNT_HEADER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.portfolio.PortfolioResponseDto;
import com.tychewealth.dto.portfolio.request.PortfolioCreateRequestDto;
import com.tychewealth.dto.portfolio.request.PortfolioUpdateRequestDto;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.service.PortfolioService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
    firstPortfolio.setName(TEST_PORTFOLIO_NAME_CORE);

    PortfolioResponseDto secondPortfolio = new PortfolioResponseDto();
    secondPortfolio.setId(8L);
    secondPortfolio.setName(TEST_PORTFOLIO_NAME_RETIREMENT);

    Page<PortfolioResponseDto> response =
        new PageImpl<>(List.of(firstPortfolio, secondPortfolio), PageRequest.of(0, 10), 2);

    when(portfolioService.listPortfolios(42L, 0, 10)).thenReturn(response);

    ResponseEntity<List<PortfolioResponseDto>> result =
        portfolioApiController.listPortfolios(42L, 0, 10);

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(CACHE_CONTROL_NO_STORE_HEADER_VALUE, result.getHeaders().getCacheControl());
    assertEquals(PRAGMA_NO_CACHE_HEADER_VALUE, result.getHeaders().getPragma());
    assertEquals(response.getContent(), result.getBody());
    assertEquals("2", result.getHeaders().getFirst(TEST_X_TOTAL_COUNT_HEADER));
    assertEquals("0", result.getHeaders().getFirst(TEST_X_PAGE_HEADER));
    assertEquals("10", result.getHeaders().getFirst(TEST_X_LIMIT_HEADER));
    assertEquals("false", result.getHeaders().getFirst(TEST_X_HAS_NEXT_HEADER));
    verify(portfolioService).listPortfolios(42L, 0, 10);
  }

  @Test
  void listPortfoliosUsesEffectivePaginationValuesInHeaders() {
    PortfolioResponseDto portfolio = new PortfolioResponseDto();
    portfolio.setId(7L);
    portfolio.setName(TEST_PORTFOLIO_NAME_CORE);
    Page<PortfolioResponseDto> response =
        new PageImpl<>(List.of(portfolio), PageRequest.of(0, 10), 1);

    when(portfolioService.listPortfolios(42L, -5, 999)).thenReturn(response);

    ResponseEntity<List<PortfolioResponseDto>> result =
        portfolioApiController.listPortfolios(42L, -5, 999);

    assertEquals("0", result.getHeaders().getFirst(TEST_X_PAGE_HEADER));
    assertEquals("10", result.getHeaders().getFirst(TEST_X_LIMIT_HEADER));
    verify(portfolioService).listPortfolios(42L, -5, 999);
  }

  @Test
  void retrieveReturnsOkResponse() {
    PortfolioResponseDto response = new PortfolioResponseDto();
    response.setId(7L);
    response.setName(TEST_PORTFOLIO_NAME_CORE);

    when(portfolioService.retrieve(42L, 7L)).thenReturn(response);

    ResponseEntity<PortfolioResponseDto> result = portfolioApiController.retrieve(42L, 7L);

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(CACHE_CONTROL_NO_STORE_HEADER_VALUE, result.getHeaders().getCacheControl());
    assertEquals(PRAGMA_NO_CACHE_HEADER_VALUE, result.getHeaders().getPragma());
    assertEquals(response, result.getBody());
    verify(portfolioService).retrieve(42L, 7L);
  }

  @Test
  void createReturnsCreatedResponse() {
    PortfolioCreateRequestDto request = new PortfolioCreateRequestDto();
    request.setName(TEST_PORTFOLIO_NAME_CORE);
    request.setBaseCurrency(CurrencyCodeEnum.USD);

    PortfolioResponseDto response = new PortfolioResponseDto();
    response.setId(7L);
    response.setName(TEST_PORTFOLIO_NAME_CORE);

    when(portfolioService.create(42L, request)).thenReturn(response);

    ResponseEntity<PortfolioResponseDto> result = portfolioApiController.create(42L, request);

    assertEquals(HttpStatus.CREATED, result.getStatusCode());
    assertEquals(CACHE_CONTROL_NO_STORE_HEADER_VALUE, result.getHeaders().getCacheControl());
    assertEquals(PRAGMA_NO_CACHE_HEADER_VALUE, result.getHeaders().getPragma());
    assertEquals(response, result.getBody());
    verify(portfolioService).create(42L, request);
  }

  @Test
  void updateReturnsOkResponse() {
    PortfolioUpdateRequestDto request = new PortfolioUpdateRequestDto();
    request.setName(TEST_PORTFOLIO_NAME_CORE);
    request.setBaseCurrency(CurrencyCodeEnum.USD);

    PortfolioResponseDto response = new PortfolioResponseDto();
    response.setId(7L);
    response.setName(TEST_PORTFOLIO_NAME_CORE);

    when(portfolioService.update(42L, 7L, request)).thenReturn(response);

    ResponseEntity<PortfolioResponseDto> result = portfolioApiController.update(42L, 7L, request);

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(CACHE_CONTROL_NO_STORE_HEADER_VALUE, result.getHeaders().getCacheControl());
    assertEquals(PRAGMA_NO_CACHE_HEADER_VALUE, result.getHeaders().getPragma());
    assertEquals(response, result.getBody());
    verify(portfolioService).update(42L, 7L, request);
  }

  @Test
  void deleteReturnsNoContentResponse() {
    ResponseEntity<Void> result = portfolioApiController.delete(42L, 7L);

    assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    assertEquals(CACHE_CONTROL_NO_STORE_HEADER_VALUE, result.getHeaders().getCacheControl());
    assertEquals(PRAGMA_NO_CACHE_HEADER_VALUE, result.getHeaders().getPragma());
    verify(portfolioService).delete(42L, 7L);
  }
}

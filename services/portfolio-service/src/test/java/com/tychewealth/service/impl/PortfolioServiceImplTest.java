package com.tychewealth.service.impl;

import static com.tychewealth.constants.CommonConstants.NAME;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_MAX_RISK;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_NAME_RETIREMENT;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testdata.EntityBuilder.buildPortfolio;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.portfolio.PortfolioResponseDto;
import com.tychewealth.dto.portfolio.request.PortfolioCreateRequestDto;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.enums.InvestmentHorizonEnum;
import com.tychewealth.enums.RiskProfileEnum;
import com.tychewealth.enums.StrategyTypeEnum;
import com.tychewealth.error.exception.PortfolioException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.mapper.portfolio.PortfolioMapper;
import com.tychewealth.repository.PortfolioRepository;
import com.tychewealth.service.helper.portfolio.PortfolioValidationHelper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceImplTest {

  @Mock private PortfolioRepository portfolioRepository;
  @Mock private PortfolioMapper portfolioMapper;
  @Mock private PortfolioValidationHelper portfolioValidationHelper;

  @InjectMocks private PortfolioServiceImpl portfolioService;

  @Test
  void listPortfoliosReturnsMappedPortfoliosForUser() {
    PortfolioEntity firstPortfolio =
        buildPortfolio(
            TEST_USER_ID,
            "Core",
            CurrencyCodeEnum.USD,
            RiskProfileEnum.MEDIUM,
            StrategyTypeEnum.BALANCED,
            InvestmentHorizonEnum.MEDIUM);
    firstPortfolio.setId(7L);

    PortfolioEntity secondPortfolio =
        buildPortfolio(
            TEST_USER_ID,
            TEST_PORTFOLIO_NAME_RETIREMENT,
            CurrencyCodeEnum.EUR,
            RiskProfileEnum.LOW,
            StrategyTypeEnum.INCOME,
            InvestmentHorizonEnum.LONG);
    secondPortfolio.setId(8L);

    PortfolioResponseDto firstResponse = new PortfolioResponseDto();
    firstResponse.setId(7L);
    firstResponse.setName("Core");

    PortfolioResponseDto secondResponse = new PortfolioResponseDto();
    secondResponse.setId(8L);
    secondResponse.setName(TEST_PORTFOLIO_NAME_RETIREMENT);

    when(portfolioRepository.findByUserId(TEST_USER_ID))
        .thenReturn(List.of(firstPortfolio, secondPortfolio));
    when(portfolioMapper.toDto(firstPortfolio)).thenReturn(firstResponse);
    when(portfolioMapper.toDto(secondPortfolio)).thenReturn(secondResponse);

    List<PortfolioResponseDto> result = portfolioService.listPortfolios(TEST_USER_ID);

    assertEquals(2, result.size());
    assertEquals(List.of(firstResponse, secondResponse), result);
    verify(portfolioRepository).findByUserId(TEST_USER_ID);
  }

  @Test
  void createPersistsPortfolioAndReturnsResponse() {
    PortfolioCreateRequestDto request = new PortfolioCreateRequestDto();
    request.setName(TEST_PORTFOLIO_NAME_RETIREMENT);
    request.setBaseCurrency(CurrencyCodeEnum.EUR);
    request.setMaxRisk(TEST_PORTFOLIO_MAX_RISK);

    PortfolioEntity mappedEntity = new PortfolioEntity();
    mappedEntity.setName(TEST_PORTFOLIO_NAME_RETIREMENT);
    mappedEntity.setBaseCurrency(CurrencyCodeEnum.EUR);

    PortfolioEntity savedEntity = new PortfolioEntity();
    savedEntity.setId(10L);
    savedEntity.setUserId(42L);
    savedEntity.setName(TEST_PORTFOLIO_NAME_RETIREMENT);
    savedEntity.setBaseCurrency(CurrencyCodeEnum.EUR);

    PortfolioResponseDto response = new PortfolioResponseDto();
    response.setId(10L);
    response.setName(TEST_PORTFOLIO_NAME_RETIREMENT);
    response.setBaseCurrency(CurrencyCodeEnum.EUR);

    when(portfolioMapper.create(request)).thenReturn(mappedEntity);
    when(portfolioRepository.saveAndFlush(mappedEntity)).thenReturn(savedEntity);
    when(portfolioMapper.toDto(savedEntity)).thenReturn(response);

    PortfolioResponseDto result = portfolioService.create(42L, request);

    assertEquals(10L, result.getId());
    assertEquals(TEST_PORTFOLIO_NAME_RETIREMENT, result.getName());
    assertEquals(42L, mappedEntity.getUserId());
    verify(portfolioValidationHelper).validateCreateRequest(42L, request);
  }

  @Test
  void createTranslatesConstraintConflicts() {
    PortfolioCreateRequestDto request = new PortfolioCreateRequestDto();
    request.setName(TEST_PORTFOLIO_NAME_RETIREMENT);
    request.setBaseCurrency(CurrencyCodeEnum.EUR);

    PortfolioEntity mappedEntity = new PortfolioEntity();
    mappedEntity.setName(TEST_PORTFOLIO_NAME_RETIREMENT);

    when(portfolioMapper.create(request)).thenReturn(mappedEntity);
    DataIntegrityViolationException persistenceException =
        new DataIntegrityViolationException("duplicate");
    when(portfolioRepository.saveAndFlush(mappedEntity)).thenThrow(persistenceException);
    doThrow(
            new PortfolioException(
                com.tychewealth.error.handler.ErrorDefinition.PORTFOLIO_NAME_CONFLICT,
                java.util.Map.of(NAME, TEST_PORTFOLIO_NAME_RETIREMENT),
                HttpStatus.CONFLICT))
        .when(portfolioValidationHelper)
        .validateCreatePersistenceConflict(
            eq(persistenceException), eq(TEST_PORTFOLIO_NAME_RETIREMENT));

    PortfolioException expected =
        assertThrows(PortfolioException.class, () -> portfolioService.create(42L, request));

    assertEquals(
        ErrorDefinition.PORTFOLIO_NAME_CONFLICT
            .getDescription()
            .replace("${name:-}", TEST_PORTFOLIO_NAME_RETIREMENT),
        expected.getMessage());
    verify(portfolioValidationHelper)
        .validateCreatePersistenceConflict(
            eq(persistenceException), eq(TEST_PORTFOLIO_NAME_RETIREMENT));
  }
}

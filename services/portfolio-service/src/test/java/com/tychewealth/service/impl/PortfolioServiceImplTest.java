package com.tychewealth.service.impl;

import static com.tychewealth.constants.CommonConstants.NAME;
import static com.tychewealth.constants.CommonConstants.NAME_PLACEHOLDER;
import static com.tychewealth.constants.LogConstants.RETRIEVE_ACTION;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_ID;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_MAX_RISK;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_NAME_CORE;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_NAME_RETIREMENT;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testdata.EntityBuilder.buildPortfolio;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.portfolio.PortfolioResponseDto;
import com.tychewealth.dto.portfolio.request.PortfolioCreateRequestDto;
import com.tychewealth.dto.portfolio.request.PortfolioUpdateRequestDto;
import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.enums.InvestmentHorizonEnum;
import com.tychewealth.enums.RiskProfileEnum;
import com.tychewealth.enums.StrategyTypeEnum;
import com.tychewealth.error.exception.PortfolioException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.mapper.portfolio.PortfolioMapper;
import com.tychewealth.repository.PortfolioRepository;
import com.tychewealth.service.helper.CommonValidationHelper;
import com.tychewealth.service.helper.portfolio.PortfolioValidationHelper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceImplTest {

  private static final long TEST_SECOND_PORTFOLIO_ID = 8L;
  private static final long TEST_TOTAL_ELEMENTS = 5L;
  private static final String TEST_UPDATED_PORTFOLIO_NAME_RETIREMENT = "Updated retirement";

  @Mock private PortfolioRepository portfolioRepository;
  @Mock private PortfolioMapper portfolioMapper;
  @Mock private CommonValidationHelper commonValidationHelper;
  @Mock private PortfolioValidationHelper portfolioValidationHelper;

  @InjectMocks private PortfolioServiceImpl portfolioService;

  @Test
  void listPortfoliosReturnsMappedPortfoliosForUserInCreatedAtOrder() {
    PortfolioEntity firstPortfolio =
        buildPortfolio(
            TEST_USER_ID,
            TEST_PORTFOLIO_NAME_CORE,
            CurrencyCodeEnum.USD,
            RiskProfileEnum.MEDIUM,
            StrategyTypeEnum.BALANCED,
            InvestmentHorizonEnum.MEDIUM);
    firstPortfolio.setId(TEST_PORTFOLIO_ID);

    PortfolioEntity secondPortfolio =
        buildPortfolio(
            TEST_USER_ID,
            TEST_PORTFOLIO_NAME_RETIREMENT,
            CurrencyCodeEnum.EUR,
            RiskProfileEnum.LOW,
            StrategyTypeEnum.INCOME,
            InvestmentHorizonEnum.LONG);
    secondPortfolio.setId(TEST_SECOND_PORTFOLIO_ID);

    PortfolioResponseDto firstResponse = new PortfolioResponseDto();
    firstResponse.setId(TEST_PORTFOLIO_ID);
    firstResponse.setName(TEST_PORTFOLIO_NAME_CORE);

    PortfolioResponseDto secondResponse = new PortfolioResponseDto();
    secondResponse.setId(TEST_SECOND_PORTFOLIO_ID);
    secondResponse.setName(TEST_PORTFOLIO_NAME_RETIREMENT);

    when(portfolioRepository.findByUserId(TEST_USER_ID, PageRequest.of(0, 10, Sort.by("id"))))
        .thenReturn(
            new PageImpl<>(
                List.of(firstPortfolio, secondPortfolio),
                PageRequest.of(0, 2),
                TEST_TOTAL_ELEMENTS));
    when(portfolioMapper.toDto(firstPortfolio)).thenReturn(firstResponse);
    when(portfolioMapper.toDto(secondPortfolio)).thenReturn(secondResponse);

    Page<PortfolioResponseDto> result = portfolioService.listPortfolios(TEST_USER_ID, 0, 10);

    assertEquals(2, result.getContent().size());
    assertEquals(List.of(firstResponse, secondResponse), result.getContent());
    assertEquals(TEST_TOTAL_ELEMENTS, result.getTotalElements());
    verify(portfolioRepository).findByUserId(TEST_USER_ID, PageRequest.of(0, 10, Sort.by("id")));
  }

  @Test
  void retrieveReturnsMappedOwnedPortfolio() {
    PortfolioEntity portfolio = new PortfolioEntity();
    portfolio.setId(TEST_PORTFOLIO_ID);
    portfolio.setUserId(TEST_USER_ID);
    portfolio.setName(TEST_PORTFOLIO_NAME_CORE);

    PortfolioResponseDto response = new PortfolioResponseDto();
    response.setId(TEST_PORTFOLIO_ID);
    response.setName(TEST_PORTFOLIO_NAME_CORE);

    when(commonValidationHelper.validateOwnedPortfolio(
            TEST_USER_ID, TEST_PORTFOLIO_ID, RETRIEVE_ACTION))
        .thenReturn(portfolio);
    when(portfolioMapper.toDto(portfolio)).thenReturn(response);

    PortfolioResponseDto result = portfolioService.retrieve(TEST_USER_ID, TEST_PORTFOLIO_ID);

    assertEquals(TEST_PORTFOLIO_ID, result.getId());
    assertEquals(TEST_PORTFOLIO_NAME_CORE, result.getName());
    verify(commonValidationHelper).validateAuthenticatedUser(TEST_USER_ID);
    verify(commonValidationHelper)
        .validateOwnedPortfolio(TEST_USER_ID, TEST_PORTFOLIO_ID, RETRIEVE_ACTION);
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

    PortfolioResponseDto result = portfolioService.create(TEST_USER_ID, request);

    assertEquals(10L, result.getId());
    assertEquals(TEST_PORTFOLIO_NAME_RETIREMENT, result.getName());
    assertEquals(TEST_USER_ID, mappedEntity.getUserId());
    verify(commonValidationHelper).validateAuthenticatedUser(TEST_USER_ID);
    verify(portfolioValidationHelper).validateCreateRequest(TEST_USER_ID, request);
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
        .translateNamePersistenceConflict(persistenceException, TEST_PORTFOLIO_NAME_RETIREMENT);

    PortfolioException expected =
        assertThrows(PortfolioException.class, () -> portfolioService.create(42L, request));

    assertEquals(
        ErrorDefinition.PORTFOLIO_NAME_CONFLICT
            .getDescription()
            .replace(NAME_PLACEHOLDER, TEST_PORTFOLIO_NAME_RETIREMENT),
        expected.getMessage());
    verify(portfolioValidationHelper)
        .translateNamePersistenceConflict(persistenceException, TEST_PORTFOLIO_NAME_RETIREMENT);
  }

  @Test
  void updatePersistsOwnedPortfolioAndReturnsResponse() {
    PortfolioUpdateRequestDto request = new PortfolioUpdateRequestDto();
    request.setName(TEST_UPDATED_PORTFOLIO_NAME_RETIREMENT);
    request.setBaseCurrency(CurrencyCodeEnum.USD);

    PortfolioEntity existingPortfolio = new PortfolioEntity();
    existingPortfolio.setId(TEST_PORTFOLIO_ID);
    existingPortfolio.setUserId(TEST_USER_ID);
    existingPortfolio.setName(TEST_PORTFOLIO_NAME_RETIREMENT);

    PortfolioResponseDto response = new PortfolioResponseDto();
    response.setId(TEST_PORTFOLIO_ID);
    response.setName(TEST_UPDATED_PORTFOLIO_NAME_RETIREMENT);
    response.setBaseCurrency(CurrencyCodeEnum.USD);

    when(portfolioValidationHelper.validateUpdateRequest(TEST_USER_ID, TEST_PORTFOLIO_ID, request))
        .thenReturn(existingPortfolio);
    when(portfolioRepository.saveAndFlush(existingPortfolio)).thenReturn(existingPortfolio);
    when(portfolioMapper.toDto(existingPortfolio)).thenReturn(response);

    PortfolioResponseDto result = portfolioService.update(TEST_USER_ID, TEST_PORTFOLIO_ID, request);

    assertEquals(TEST_PORTFOLIO_ID, result.getId());
    assertEquals(TEST_UPDATED_PORTFOLIO_NAME_RETIREMENT, result.getName());
    verify(portfolioMapper).update(request, existingPortfolio);
    verify(portfolioRepository).saveAndFlush(existingPortfolio);
    verify(commonValidationHelper).validateAuthenticatedUser(TEST_USER_ID);
  }

  @Test
  void updateTranslatesConstraintConflicts() {
    PortfolioUpdateRequestDto request = new PortfolioUpdateRequestDto();
    request.setName(TEST_PORTFOLIO_NAME_RETIREMENT);

    PortfolioEntity existingPortfolio = new PortfolioEntity();
    existingPortfolio.setId(TEST_PORTFOLIO_ID);
    existingPortfolio.setUserId(TEST_USER_ID);
    existingPortfolio.setName(TEST_PORTFOLIO_NAME_RETIREMENT);

    DataIntegrityViolationException persistenceException =
        new DataIntegrityViolationException("duplicate");

    when(portfolioValidationHelper.validateUpdateRequest(TEST_USER_ID, TEST_PORTFOLIO_ID, request))
        .thenReturn(existingPortfolio);
    when(portfolioRepository.saveAndFlush(existingPortfolio)).thenThrow(persistenceException);
    doThrow(
            new PortfolioException(
                ErrorDefinition.PORTFOLIO_NAME_CONFLICT,
                java.util.Map.of(NAME, TEST_PORTFOLIO_NAME_RETIREMENT),
                HttpStatus.CONFLICT))
        .when(portfolioValidationHelper)
        .translateNamePersistenceConflict(persistenceException, TEST_PORTFOLIO_NAME_RETIREMENT);

    PortfolioException expected =
        assertThrows(
            PortfolioException.class,
            () -> portfolioService.update(TEST_USER_ID, TEST_PORTFOLIO_ID, request));

    assertEquals(
        ErrorDefinition.PORTFOLIO_NAME_CONFLICT
            .getDescription()
            .replace(NAME_PLACEHOLDER, TEST_PORTFOLIO_NAME_RETIREMENT),
        expected.getMessage());
    verify(portfolioValidationHelper)
        .translateNamePersistenceConflict(persistenceException, TEST_PORTFOLIO_NAME_RETIREMENT);
  }

  @Test
  void deleteSoftDeletesOwnedPortfolio() {
    PortfolioEntity portfolio = new PortfolioEntity();
    portfolio.setId(TEST_PORTFOLIO_ID);
    portfolio.setUserId(TEST_USER_ID);
    AssetEntity asset = new AssetEntity();
    portfolio.getAssets().add(asset);

    when(portfolioRepository.findByIdAndUserId(TEST_PORTFOLIO_ID, TEST_USER_ID))
        .thenReturn(Optional.of(portfolio));

    portfolioService.delete(TEST_USER_ID, TEST_PORTFOLIO_ID);

    verify(commonValidationHelper).validateAuthenticatedUser(TEST_USER_ID);
    verify(portfolioRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    assertNotNull(portfolio.getDeletedAt());
    assertNotNull(asset.getDeletedAt());
    assertEquals(portfolio.getDeletedAt(), asset.getDeletedAt());
  }

  @Test
  void deletePreservesDeletedAtForAlreadyDeletedAsset() {
    PortfolioEntity portfolio = new PortfolioEntity();
    portfolio.setId(TEST_PORTFOLIO_ID);
    portfolio.setUserId(TEST_USER_ID);
    LocalDateTime originalDeletedAt = LocalDateTime.now().minusDays(1);
    AssetEntity asset = new AssetEntity();
    asset.setDeletedAt(originalDeletedAt);
    portfolio.getAssets().add(asset);

    when(portfolioRepository.findByIdAndUserId(TEST_PORTFOLIO_ID, TEST_USER_ID))
        .thenReturn(Optional.of(portfolio));

    portfolioService.delete(TEST_USER_ID, TEST_PORTFOLIO_ID);

    assertNotNull(portfolio.getDeletedAt());
    assertEquals(originalDeletedAt, asset.getDeletedAt());
  }

  @Test
  void deleteDoesNothingWhenPortfolioDoesNotBelongToUser() {
    when(portfolioRepository.findByIdAndUserId(TEST_PORTFOLIO_ID, TEST_USER_ID))
        .thenReturn(Optional.empty());

    portfolioService.delete(TEST_USER_ID, TEST_PORTFOLIO_ID);

    verify(commonValidationHelper).validateAuthenticatedUser(TEST_USER_ID);
    verify(portfolioRepository, never()).delete(org.mockito.ArgumentMatchers.any());
  }
}

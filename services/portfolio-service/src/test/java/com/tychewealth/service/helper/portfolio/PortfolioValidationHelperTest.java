package com.tychewealth.service.helper.portfolio;

import static com.tychewealth.constants.CommonConstants.NAME;
import static com.tychewealth.constants.TestConstants.TEST_MAX_PORTFOLIOS_PER_USER;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_NAME_RETIREMENT;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.portfolio.request.PortfolioCreateRequestDto;
import com.tychewealth.error.exception.PortfolioException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.PortfolioRepository;
import com.tychewealth.service.helper.CommonValidationHelper;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PortfolioValidationHelperTest {

  @Mock private PortfolioRepository portfolioRepository;
  @Mock private CommonValidationHelper commonValidationHelper;

  @Test
  void validateCreateRequestPassesWhenUserExistsAndPortfolioNameIsAvailable() {
    PortfolioCreateRequestDto request = new PortfolioCreateRequestDto();
    request.setName(TEST_PORTFOLIO_NAME_RETIREMENT);
    PortfolioValidationHelper helper =
        new PortfolioValidationHelper(commonValidationHelper, portfolioRepository);

    when(portfolioRepository.countByUserId(TEST_USER_ID)).thenReturn(0L);
    when(portfolioRepository.existsByUserIdAndName(TEST_USER_ID, TEST_PORTFOLIO_NAME_RETIREMENT))
        .thenReturn(false);

    helper.validateCreateRequest(TEST_USER_ID, request);

    verify(portfolioRepository).countByUserId(TEST_USER_ID);
    verify(portfolioRepository).existsByUserIdAndName(TEST_USER_ID, TEST_PORTFOLIO_NAME_RETIREMENT);
  }

  @Test
  void validateCreateRequestThrowsConflictWhenPortfolioNameAlreadyExists() {
    PortfolioCreateRequestDto request = new PortfolioCreateRequestDto();
    request.setName(TEST_PORTFOLIO_NAME_RETIREMENT);
    PortfolioValidationHelper helper =
        new PortfolioValidationHelper(commonValidationHelper, portfolioRepository);

    when(portfolioRepository.countByUserId(TEST_USER_ID)).thenReturn(0L);
    when(portfolioRepository.existsByUserIdAndName(TEST_USER_ID, TEST_PORTFOLIO_NAME_RETIREMENT))
        .thenReturn(true);

    PortfolioException exception =
        assertThrows(
            PortfolioException.class, () -> helper.validateCreateRequest(TEST_USER_ID, request));

    assertEquals(ErrorDefinition.PORTFOLIO_NAME_CONFLICT, exception.getErrorDefinition());
    assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus());
    assertEquals(TEST_PORTFOLIO_NAME_RETIREMENT, exception.getDescription().get(NAME));
  }

  @Test
  void validateCreateNameConflictThrowsConflictWhenPortfolioNameAlreadyExists() {
    PortfolioValidationHelper helper =
        new PortfolioValidationHelper(commonValidationHelper, portfolioRepository);

    when(portfolioRepository.existsByUserIdAndName(TEST_USER_ID, TEST_PORTFOLIO_NAME_RETIREMENT))
        .thenReturn(true);

    PortfolioException exception =
        assertThrows(
            PortfolioException.class,
            () -> helper.validateCreateNameConflict(TEST_USER_ID, TEST_PORTFOLIO_NAME_RETIREMENT));

    assertEquals(ErrorDefinition.PORTFOLIO_NAME_CONFLICT, exception.getErrorDefinition());
    assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus());
    assertEquals(TEST_PORTFOLIO_NAME_RETIREMENT, exception.getDescription().get(NAME));
  }

  @Test
  void validateCreateLimitThrowsConflictWhenUserAlreadyHasMaximumPortfolios() {
    PortfolioValidationHelper helper =
        new PortfolioValidationHelper(commonValidationHelper, portfolioRepository);

    when(portfolioRepository.countByUserId(TEST_USER_ID)).thenReturn(TEST_MAX_PORTFOLIOS_PER_USER);

    PortfolioException exception =
        assertThrows(PortfolioException.class, () -> helper.validateCreateLimit(TEST_USER_ID));

    assertEquals(ErrorDefinition.PORTFOLIO_LIMIT_REACHED, exception.getErrorDefinition());
    assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus());
  }

  @Test
  void translateNamePersistenceConflictReturnsPortfolioExceptionForUniqueConstraint() {
    PortfolioValidationHelper helper =
        new PortfolioValidationHelper(commonValidationHelper, portfolioRepository);
    DataIntegrityViolationException exception =
        new DataIntegrityViolationException(
            "duplicate",
            new ConstraintViolationException(
                "could not execute statement", null, "uq_portfolio_user_id_name_index"));

    PortfolioException result =
        helper.translateNamePersistenceConflict(exception, TEST_PORTFOLIO_NAME_RETIREMENT);

    assertEquals(ErrorDefinition.PORTFOLIO_NAME_CONFLICT, result.getErrorDefinition());
    assertEquals(HttpStatus.CONFLICT, result.getHttpStatus());
    assertEquals(TEST_PORTFOLIO_NAME_RETIREMENT, result.getDescription().get(NAME));
  }

  @Test
  void translateNamePersistenceConflictRethrowsOriginalExceptionWhenConstraintIsUnrelated() {
    PortfolioValidationHelper helper =
        new PortfolioValidationHelper(commonValidationHelper, portfolioRepository);
    DataIntegrityViolationException exception =
        new DataIntegrityViolationException("some unrelated persistence failure");

    DataIntegrityViolationException thrown =
        assertThrows(
            DataIntegrityViolationException.class,
            () ->
                helper.translateNamePersistenceConflict(exception, TEST_PORTFOLIO_NAME_RETIREMENT));

    assertSame(exception, thrown);
  }
}

package com.tychewealth.service.helper;

import static com.tychewealth.constants.LogConstants.MISSING_AUTHENTICATED_USER_MESSAGE;
import static com.tychewealth.constants.LogConstants.RETRIEVE_ACTION;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_ID;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.error.exception.PortfolioException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.PortfolioRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class CommonValidationHelperTest {

  @Mock private PortfolioRepository portfolioRepository;

  @Test
  void validateAuthenticatedUserThrowsBadRequestWhenAuthenticatedUserIsMissing() {
    CommonValidationHelper helper = new CommonValidationHelper(portfolioRepository);

    PortfolioException exception =
        assertThrows(PortfolioException.class, () -> helper.validateAuthenticatedUser(null));

    assertEquals(ErrorDefinition.GENERIC_BAD_REQUEST, exception.getErrorDefinition());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    assertEquals(MISSING_AUTHENTICATED_USER_MESSAGE, exception.getDescription().get("error"));
  }

  @Test
  void validateOwnedPortfolioReturnsPortfolioWhenOwnershipMatches() {
    CommonValidationHelper helper = new CommonValidationHelper(portfolioRepository);
    PortfolioEntity portfolio = new PortfolioEntity();
    portfolio.setId(TEST_PORTFOLIO_ID);
    portfolio.setUserId(TEST_USER_ID);

    when(portfolioRepository.findByIdAndUserId(TEST_PORTFOLIO_ID, TEST_USER_ID))
        .thenReturn(Optional.of(portfolio));

    PortfolioEntity result =
        helper.validateOwnedPortfolio(TEST_USER_ID, TEST_PORTFOLIO_ID, RETRIEVE_ACTION);

    assertSame(portfolio, result);
  }

  @Test
  void validateOwnedPortfolioThrowsNotFoundWhenOwnershipDoesNotMatch() {
    CommonValidationHelper helper = new CommonValidationHelper(portfolioRepository);

    when(portfolioRepository.findByIdAndUserId(TEST_PORTFOLIO_ID, TEST_USER_ID))
        .thenReturn(Optional.empty());

    PortfolioException exception =
        assertThrows(
            PortfolioException.class,
            () -> helper.validateOwnedPortfolio(TEST_USER_ID, TEST_PORTFOLIO_ID, RETRIEVE_ACTION));

    assertEquals(ErrorDefinition.PORTFOLIO_NOT_FOUND, exception.getErrorDefinition());
    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
    assertEquals(Map.of(), exception.getDescription());
  }
}

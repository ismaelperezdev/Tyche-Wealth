package com.tychewealth.service.helper;

import static com.tychewealth.constants.LogConstants.CREATE_ACTION;
import static com.tychewealth.constants.LogConstants.MISSING_AUTHENTICATED_USER_MESSAGE;
import static com.tychewealth.constants.LogConstants.PORTFOLIO;
import static com.tychewealth.constants.LogConstants.PORTFOLIO_ID;
import static com.tychewealth.constants.LogConstants.PORTFOLIO_NOT_FOUND_MESSAGE;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;
import static com.tychewealth.constants.LogConstants.SYSTEM;
import static com.tychewealth.constants.LogConstants.USER_ID;
import static com.tychewealth.error.handler.ErrorDefinition.PORTFOLIO_NOT_FOUND;

import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.error.exception.PortfolioException;
import com.tychewealth.repository.PortfolioRepository;
import com.tychewealth.utils.Utils;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Provides validation shared by portfolio and asset service operations.
 *
 * <p>Validates the presence of the authenticated user and resolves portfolios owned by that user,
 * translating missing resources into the service's standard domain exception.
 */
@Slf4j
@Component
@AllArgsConstructor
public class CommonValidationHelper {

  private final PortfolioRepository portfolioRepository;

  public void validateAuthenticatedUser(Long userId) {
    if (userId != null) {
      return;
    }

    log.warn(REQUEST_CONFLICT, SYSTEM, CREATE_ACTION, MISSING_AUTHENTICATED_USER_MESSAGE);
    throw Utils.genericBadRequest(MISSING_AUTHENTICATED_USER_MESSAGE);
  }

  public PortfolioEntity validateOwnedPortfolio(Long userId, Long portfolioId, String action) {
    return portfolioRepository
        .findByIdAndUserId(portfolioId, userId)
        .orElseThrow(
            () -> {
              log.warn(
                  REQUEST_CONFLICT + PORTFOLIO_ID + USER_ID,
                  PORTFOLIO,
                  action,
                  PORTFOLIO_NOT_FOUND_MESSAGE,
                  portfolioId,
                  userId);
              return new PortfolioException(PORTFOLIO_NOT_FOUND, Map.of(), HttpStatus.NOT_FOUND);
            });
  }
}

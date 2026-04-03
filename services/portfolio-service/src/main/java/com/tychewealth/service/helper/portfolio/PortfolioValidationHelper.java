package com.tychewealth.service.helper.portfolio;

import static com.tychewealth.constants.CommonConstants.*;
import static com.tychewealth.constants.LogConstants.CREATE_ACTION;
import static com.tychewealth.constants.LogConstants.MISSING_AUTHENTICATED_USER_MESSAGE;
import static com.tychewealth.constants.LogConstants.PORTFOLIO;
import static com.tychewealth.constants.LogConstants.PORTFOLIO_ID;
import static com.tychewealth.constants.LogConstants.PORTFOLIO_LIMIT_REACHED_MESSAGE;
import static com.tychewealth.constants.LogConstants.PORTFOLIO_NAME;
import static com.tychewealth.constants.LogConstants.PORTFOLIO_NAME_ALREADY_EXISTS_MESSAGE;
import static com.tychewealth.constants.LogConstants.PORTFOLIO_NOT_FOUND_MESSAGE;
import static com.tychewealth.constants.LogConstants.PORTFOLIO_PERSISTENCE_CONFLICT_MESSAGE;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;
import static com.tychewealth.constants.LogConstants.UNKNOWN_PERSISTENCE_CONFLICT_MESSAGE;
import static com.tychewealth.constants.LogConstants.UPDATE_ACTION;
import static com.tychewealth.constants.LogConstants.USER_ID;
import static com.tychewealth.error.handler.ErrorDefinition.GENERIC_BAD_REQUEST;
import static com.tychewealth.error.handler.ErrorDefinition.PORTFOLIO_LIMIT_REACHED;
import static com.tychewealth.error.handler.ErrorDefinition.PORTFOLIO_NAME_CONFLICT;
import static com.tychewealth.error.handler.ErrorDefinition.PORTFOLIO_NOT_FOUND;

import com.tychewealth.dto.portfolio.request.PortfolioCreateRequestDto;
import com.tychewealth.dto.portfolio.request.PortfolioUpdateRequestDto;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.error.exception.PortfolioException;
import com.tychewealth.repository.PortfolioRepository;
import com.tychewealth.utils.Utils;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class PortfolioValidationHelper {

  private static final long MAX_PORTFOLIOS_PER_USER = 5L;
  private static final String PORTFOLIO_UNIQUE_CONSTRAINT = "uq_portfolio_user_id_name";

  private final PortfolioRepository portfolioRepository;

  public void validateCreateRequest(Long userId, PortfolioCreateRequestDto createRequest) {
    String portfolioName = createRequest == null ? null : createRequest.getName();
    validateAuthenticatedUser(userId);
    validateCreateLimit(userId);
    validateCreateNameConflict(userId, portfolioName);
  }

  public PortfolioEntity validateUpdateRequest(
      Long userId, Long portfolioId, PortfolioUpdateRequestDto updateRequest) {
    String portfolioName = updateRequest == null ? null : updateRequest.getName();
    validateAuthenticatedUser(userId);
    PortfolioEntity portfolio = validateOwnedPortfolio(userId, portfolioId);
    validateUpdateNameConflict(userId, portfolioId, portfolioName);
    return portfolio;
  }

  public void validateAuthenticatedUser(Long userId) {
    if (userId != null) {
      return;
    }

    log.warn(REQUEST_CONFLICT, PORTFOLIO, CREATE_ACTION, MISSING_AUTHENTICATED_USER_MESSAGE);
    throw new PortfolioException(
        GENERIC_BAD_REQUEST,
        Map.of(ERROR, MISSING_AUTHENTICATED_USER_MESSAGE),
        HttpStatus.BAD_REQUEST);
  }

  public void validateCreateLimit(Long userId) {
    long currentPortfolioCount = portfolioRepository.countByUserId(userId);
    if (currentPortfolioCount >= MAX_PORTFOLIOS_PER_USER) {
      log.warn(
          REQUEST_CONFLICT + USER_ID,
          PORTFOLIO,
          CREATE_ACTION,
          PORTFOLIO_LIMIT_REACHED_MESSAGE,
          userId);
      throw new PortfolioException(PORTFOLIO_LIMIT_REACHED, Map.of(), HttpStatus.CONFLICT);
    }
  }

  public void validateCreateNameConflict(Long userId, String portfolioName) {
    if (!portfolioRepository.existsByUserIdAndName(userId, portfolioName)) {
      return;
    }

    log.warn(
        REQUEST_CONFLICT + PORTFOLIO_NAME + USER_ID,
        PORTFOLIO,
        CREATE_ACTION,
        PORTFOLIO_NAME_ALREADY_EXISTS_MESSAGE,
        portfolioName,
        userId);
    throw new PortfolioException(
        PORTFOLIO_NAME_CONFLICT,
        Map.of(NAME, portfolioName == null ? "" : portfolioName),
        HttpStatus.CONFLICT);
  }

  public PortfolioEntity validateOwnedPortfolio(Long userId, Long portfolioId) {
    return portfolioRepository
        .findByIdAndUserId(portfolioId, userId)
        .orElseThrow(
            () -> {
              log.warn(
                  REQUEST_CONFLICT + PORTFOLIO_ID + USER_ID,
                  PORTFOLIO,
                  UPDATE_ACTION,
                  PORTFOLIO_NOT_FOUND_MESSAGE,
                  portfolioId,
                  userId);
              return new PortfolioException(PORTFOLIO_NOT_FOUND, Map.of(), HttpStatus.NOT_FOUND);
            });
  }

  public void validateUpdateNameConflict(Long userId, Long portfolioId, String portfolioName) {
    portfolioRepository
        .findByUserIdAndName(userId, portfolioName)
        .filter(existingPortfolio -> !existingPortfolio.getId().equals(portfolioId))
        .ifPresent(
            existingPortfolio -> {
              log.warn(
                  REQUEST_CONFLICT + PORTFOLIO_NAME + PORTFOLIO_ID + USER_ID,
                  PORTFOLIO,
                  UPDATE_ACTION,
                  PORTFOLIO_NAME_ALREADY_EXISTS_MESSAGE,
                  portfolioName,
                  portfolioId,
                  userId);
              throw new PortfolioException(
                  PORTFOLIO_NAME_CONFLICT,
                  Map.of(NAME, portfolioName == null ? "" : portfolioName),
                  HttpStatus.CONFLICT);
            });
  }

  public PortfolioException validateCreatePersistenceConflict(
      DataIntegrityViolationException ex, String portfolioName) {
    return validatePersistenceConflict(ex, CREATE_ACTION, portfolioName);
  }

  public PortfolioException validateUpdatePersistenceConflict(
      DataIntegrityViolationException ex, String portfolioName) {
    return validatePersistenceConflict(ex, UPDATE_ACTION, portfolioName);
  }

  private PortfolioException validatePersistenceConflict(
      DataIntegrityViolationException ex, String action, String portfolioName) {
    if (Utils.hasConstraintViolation(ex, PORTFOLIO_UNIQUE_CONSTRAINT)) {
      log.warn(REQUEST_CONFLICT, PORTFOLIO, action, PORTFOLIO_PERSISTENCE_CONFLICT_MESSAGE);
      return new PortfolioException(
          PORTFOLIO_NAME_CONFLICT,
          Map.of(NAME, portfolioName == null ? "" : portfolioName),
          HttpStatus.CONFLICT);
    }

    log.error(REQUEST_CONFLICT, PORTFOLIO, action, UNKNOWN_PERSISTENCE_CONFLICT_MESSAGE, ex);
    throw ex;
  }
}

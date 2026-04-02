package com.tychewealth.service.helper.portfolio;

import static com.tychewealth.constants.CommonConstants.*;
import static com.tychewealth.constants.LogConstants.CREATE_ACTION;
import static com.tychewealth.constants.LogConstants.MISSING_AUTHENTICATED_USER_MESSAGE;
import static com.tychewealth.constants.LogConstants.PORTFOLIO;
import static com.tychewealth.constants.LogConstants.PORTFOLIO_NAME;
import static com.tychewealth.constants.LogConstants.PORTFOLIO_NAME_ALREADY_EXISTS_MESSAGE;
import static com.tychewealth.constants.LogConstants.PORTFOLIO_PERSISTENCE_CONFLICT_MESSAGE;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;
import static com.tychewealth.constants.LogConstants.UNKNOWN_PERSISTENCE_CONFLICT_MESSAGE;
import static com.tychewealth.constants.LogConstants.USER_ID;
import static com.tychewealth.error.handler.ErrorDefinition.GENERIC_BAD_REQUEST;
import static com.tychewealth.error.handler.ErrorDefinition.PORTFOLIO_NAME_CONFLICT;

import com.tychewealth.dto.portfolio.request.PortfolioCreateRequestDto;
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

  private static final String PORTFOLIO_UNIQUE_CONSTRAINT = "uq_portfolio_user_id_name";

  private final PortfolioRepository portfolioRepository;

  public void validateCreateRequest(Long userId, PortfolioCreateRequestDto createRequest) {
    if (userId == null) {
      log.warn(REQUEST_CONFLICT, PORTFOLIO, CREATE_ACTION, MISSING_AUTHENTICATED_USER_MESSAGE);
      throw new PortfolioException(
          GENERIC_BAD_REQUEST,
          Map.of(ERROR, MISSING_AUTHENTICATED_USER_MESSAGE),
          HttpStatus.BAD_REQUEST);
    }

    String portfolioName = createRequest == null ? null : createRequest.getName();
    if (portfolioRepository.existsByUserIdAndName(userId, portfolioName)) {
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
  }

  public PortfolioException validateCreatePersistenceConflict(
      DataIntegrityViolationException ex, String portfolioName) {
    if (Utils.hasConstraintViolation(ex, PORTFOLIO_UNIQUE_CONSTRAINT)) {
      log.warn(REQUEST_CONFLICT, PORTFOLIO, CREATE_ACTION, PORTFOLIO_PERSISTENCE_CONFLICT_MESSAGE);
      return new PortfolioException(
          PORTFOLIO_NAME_CONFLICT,
          Map.of(NAME, portfolioName == null ? "" : portfolioName),
          HttpStatus.CONFLICT);
    }

    log.error(REQUEST_CONFLICT, PORTFOLIO, CREATE_ACTION, UNKNOWN_PERSISTENCE_CONFLICT_MESSAGE, ex);
    throw ex;
  }
}

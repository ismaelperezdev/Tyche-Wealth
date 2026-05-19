package com.tychewealth.service.helper.asset;

import static com.tychewealth.constants.CommonConstants.NAME;
import static com.tychewealth.constants.LogConstants.ASSET;
import static com.tychewealth.constants.LogConstants.ASSET_LIMIT_REACHED_MESSAGE;
import static com.tychewealth.constants.LogConstants.ASSET_NAME_ALREADY_EXISTS_MESSAGE;
import static com.tychewealth.constants.LogConstants.ASSET_NOT_FOUND_MESSAGE;
import static com.tychewealth.constants.LogConstants.ASSET_PERSISTENCE_CONFLICT_MESSAGE;
import static com.tychewealth.constants.LogConstants.BASE_LOG;
import static com.tychewealth.constants.LogConstants.CREATE_ACTION;
import static com.tychewealth.constants.LogConstants.PORTFOLIO_ID;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;
import static com.tychewealth.constants.LogConstants.RETRIEVE_ACTION;
import static com.tychewealth.constants.LogConstants.UNKNOWN_PERSISTENCE_CONFLICT_MESSAGE;
import static com.tychewealth.constants.PersistenceConstants.ASSET_SYMBOL_UNIQUE_CONSTRAINT;
import static com.tychewealth.error.handler.ErrorDefinition.ASSET_LIMIT_REACHED;
import static com.tychewealth.error.handler.ErrorDefinition.ASSET_NAME_CONFLICT;
import static com.tychewealth.error.handler.ErrorDefinition.ASSET_NOT_FOUND;
import static com.tychewealth.error.handler.ErrorDefinition.ASSET_SYMBOL_CONFLICT;

import com.tychewealth.entity.AssetEntity;
import com.tychewealth.error.exception.PortfolioException;
import com.tychewealth.repository.AssetRepository;
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
public class AssetValidationHelper {

  private static final long MAX_ASSETS_PER_PORTFOLIO = 200L;

  private final AssetRepository assetRepository;

  public void validateCreateLimit(Long portfolioId) {
    long currentAssetCount = assetRepository.findByPortfolioId(portfolioId).size();
    if (currentAssetCount >= MAX_ASSETS_PER_PORTFOLIO) {
      log.warn(
          REQUEST_CONFLICT + PORTFOLIO_ID,
          ASSET,
          CREATE_ACTION,
          ASSET_LIMIT_REACHED_MESSAGE,
          portfolioId);
      throw new PortfolioException(ASSET_LIMIT_REACHED, Map.of(), HttpStatus.CONFLICT);
    }
  }

  public void validateCreateNameConflict(Long portfolioId, String assetName) {
    if (!assetRepository.existsByPortfolioIdAndName(portfolioId, assetName)) {
      return;
    }

    log.warn(
        REQUEST_CONFLICT + PORTFOLIO_ID,
        ASSET,
        CREATE_ACTION,
        ASSET_NAME_ALREADY_EXISTS_MESSAGE,
        portfolioId);
    throw new PortfolioException(
        ASSET_NAME_CONFLICT, Map.of(NAME, assetName == null ? "" : assetName), HttpStatus.CONFLICT);
  }

  public AssetEntity validateRetrievedAssetExists(Long portfolioId, Long assetId) {
    return assetRepository
        .findByIdAndPortfolioId(assetId, portfolioId)
        .orElseThrow(
            () -> {
              log.warn(
                  REQUEST_CONFLICT + PORTFOLIO_ID,
                  ASSET,
                  RETRIEVE_ACTION,
                  ASSET_NOT_FOUND_MESSAGE,
                  portfolioId);
              return new PortfolioException(ASSET_NOT_FOUND, Map.of(), HttpStatus.NOT_FOUND);
            });
  }

  public PortfolioException translateSymbolPersistenceConflict(
      DataIntegrityViolationException ex, Long portfolioId, String symbol) {
    if (Utils.hasConstraintViolation(ex, ASSET_SYMBOL_UNIQUE_CONSTRAINT)) {
      log.warn(BASE_LOG + PORTFOLIO_ID, ASSET, ASSET_PERSISTENCE_CONFLICT_MESSAGE, portfolioId);
      return new PortfolioException(
          ASSET_SYMBOL_CONFLICT, Map.of(NAME, symbol == null ? "" : symbol), HttpStatus.CONFLICT);
    }

    log.error(
        BASE_LOG + PORTFOLIO_ID, ASSET, UNKNOWN_PERSISTENCE_CONFLICT_MESSAGE, portfolioId, ex);
    throw ex;
  }
}

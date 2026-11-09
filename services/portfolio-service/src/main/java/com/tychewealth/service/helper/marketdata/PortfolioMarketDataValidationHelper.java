package com.tychewealth.service.helper.marketdata;

import static com.tychewealth.constants.CommonConstants.ERROR;
import static com.tychewealth.constants.CommonConstants.EXPECTED;
import static com.tychewealth.constants.CommonConstants.RECEIVED;
import static com.tychewealth.constants.PortfolioLimits.MAX_ASSETS_PER_PORTFOLIO;
import static com.tychewealth.constants.PortfolioLimits.MAX_PORTFOLIOS_PER_USER;
import static com.tychewealth.error.handler.ErrorDefinition.ASSET_NOT_FOUND;
import static com.tychewealth.error.handler.ErrorDefinition.PORTFOLIO_MARKET_DATA_ID_INVALID;
import static com.tychewealth.error.handler.ErrorDefinition.PORTFOLIO_MARKET_DATA_RESULT_LIMIT_EXCEEDED;
import static com.tychewealth.error.handler.ErrorDefinition.PORTFOLIO_MARKET_DATA_SELECTION_EMPTY;
import static com.tychewealth.error.handler.ErrorDefinition.PORTFOLIO_MARKET_DATA_SELECTION_INVALID;
import static com.tychewealth.error.handler.ErrorDefinition.PORTFOLIO_MARKET_DATA_SELECTION_LIMIT_EXCEEDED;
import static com.tychewealth.error.handler.ErrorDefinition.PORTFOLIO_MARKET_DATA_TIME_RANGE_INVALID;
import static com.tychewealth.error.handler.ErrorDefinition.PORTFOLIO_NOT_FOUND;

import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.error.exception.PortfolioException;
import com.tychewealth.marketdataclient.dto.LatestMarketPrice;
import com.tychewealth.marketdataclient.dto.MarketDataPage;
import com.tychewealth.marketdataclient.dto.PriceCheckpoint;
import com.tychewealth.service.helper.CommonValidationHelper;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Validates market-data selections and confirms every requested ID was resolved for the user. */
@Component
public class PortfolioMarketDataValidationHelper {

  private final CommonValidationHelper commonValidationHelper;
  private final int maxResults;

  public PortfolioMarketDataValidationHelper(
      CommonValidationHelper commonValidationHelper,
      @Value("${app.market-data.max-results:5000}") int maxResults) {
    this.commonValidationHelper = commonValidationHelper;
    if (maxResults < 1) {
      throw new IllegalArgumentException("app.market-data.max-results must be positive");
    }
    this.maxResults = maxResults;
  }

  public void validateLatestPriceCount(MarketDataPage<LatestMarketPrice> page) {
    if (page.totalCount() > maxResults) {
      throw resultLimitExceeded(page.totalCount());
    }
  }

  public void validateHistoryCount(MarketDataPage<PriceCheckpoint> page) {
    if (page.totalCount() > page.content().size()) {
      throw resultLimitExceeded(page.totalCount());
    }
  }

  public void validateExpandedHistoryCount(long resultCount) {
    if (resultCount > maxResults) {
      throw resultLimitExceeded(resultCount);
    }
  }

  private PortfolioException resultLimitExceeded(long received) {
    return new PortfolioException(
        PORTFOLIO_MARKET_DATA_RESULT_LIMIT_EXCEEDED,
        Map.of(EXPECTED, String.valueOf(maxResults), RECEIVED, String.valueOf(received)),
        HttpStatus.UNPROCESSABLE_ENTITY);
  }

  public void validateRequest(
      Long userId, List<Long> portfolioIds, List<Long> assetIds, Instant from, Instant to) {
    commonValidationHelper.validateAuthenticatedUser(userId);

    if ((portfolioIds == null) == (assetIds == null)) {
      throw validationError(
          PORTFOLIO_MARKET_DATA_SELECTION_INVALID,
          "Portfolios and assets cannot be selected at the same time");
    }

    if (portfolioIds != null) {
      validateIds(portfolioIds, MAX_PORTFOLIOS_PER_USER);
    } else {
      validateIds(assetIds, MAX_ASSETS_PER_PORTFOLIO);
    }
    if (from != null && to != null && from.isAfter(to)) {
      throw validationError(PORTFOLIO_MARKET_DATA_TIME_RANGE_INVALID, "from must not be after to");
    }
  }

  public void validatePortfoliosResolved(
      List<Long> requestedIds, List<PortfolioEntity> portfolios) {
    Set<Long> requestedIdSet = new HashSet<>(requestedIds);
    Set<Long> resolvedIdSet =
        portfolios.stream().map(PortfolioEntity::getId).collect(Collectors.toSet());
    if (!resolvedIdSet.containsAll(requestedIdSet)) {
      throw new PortfolioException(PORTFOLIO_NOT_FOUND, Map.of(), HttpStatus.NOT_FOUND);
    }
  }

  public void validateAssetsResolved(List<Long> requestedIds, List<AssetEntity> assets) {
    Set<Long> requestedIdSet = new HashSet<>(requestedIds);
    Set<Long> resolvedIdSet = assets.stream().map(AssetEntity::getId).collect(Collectors.toSet());
    if (!resolvedIdSet.containsAll(requestedIdSet)) {
      throw new PortfolioException(ASSET_NOT_FOUND, Map.of(), HttpStatus.NOT_FOUND);
    }
  }

  private void validateIds(List<Long> ids, int maximum) {
    if (ids.isEmpty()) {
      throw validationError(
          PORTFOLIO_MARKET_DATA_SELECTION_EMPTY, "The selected ID list must not be empty");
    }

    if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
      throw validationError(PORTFOLIO_MARKET_DATA_ID_INVALID, "Selected IDs must be positive");
    }

    if (ids.size() > maximum) {
      throw new PortfolioException(
          PORTFOLIO_MARKET_DATA_SELECTION_LIMIT_EXCEEDED,
          Map.of(EXPECTED, String.valueOf(maximum)),
          HttpStatus.BAD_REQUEST);
    }
  }

  private PortfolioException validationError(
      com.tychewealth.error.handler.ErrorDefinition definition, String message) {
    return new PortfolioException(definition, Map.of(ERROR, message), HttpStatus.BAD_REQUEST);
  }
}

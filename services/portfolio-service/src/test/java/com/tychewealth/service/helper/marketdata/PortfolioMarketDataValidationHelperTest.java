package com.tychewealth.service.helper.marketdata;

import static com.tychewealth.constants.TestConstants.TEST_ASSET_ID;
import static com.tychewealth.constants.TestConstants.TEST_MAX_ASSETS_PER_PORTFOLIO;
import static com.tychewealth.constants.TestConstants.TEST_MAX_PORTFOLIOS_PER_USER;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_ID;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.error.exception.PortfolioException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.service.helper.CommonValidationHelper;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PortfolioMarketDataValidationHelperTest {

  @Mock private CommonValidationHelper commonValidationHelper;

  private PortfolioMarketDataValidationHelper validationHelper;

  @BeforeEach
  void setUp() {
    validationHelper = new PortfolioMarketDataValidationHelper(commonValidationHelper, 5000);
  }

  @Test
  void rejectsRequestWithBothPortfolioAndAssetSelections() {
    PortfolioException exception =
        assertThrows(
            PortfolioException.class,
            () ->
                validationHelper.validateRequest(
                    TEST_USER_ID, List.of(TEST_PORTFOLIO_ID), List.of(TEST_ASSET_ID), null, null));

    assertEquals(
        ErrorDefinition.PORTFOLIO_MARKET_DATA_SELECTION_INVALID, exception.getErrorDefinition());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }

  @Test
  void rejectsRequestWithoutASelection() {
    PortfolioException exception =
        assertThrows(
            PortfolioException.class,
            () -> validationHelper.validateRequest(TEST_USER_ID, null, null, null, null));

    assertEquals(
        ErrorDefinition.PORTFOLIO_MARKET_DATA_SELECTION_INVALID, exception.getErrorDefinition());
  }

  @Test
  void rejectsEmptyOrInvalidIdLists() {
    assertThrows(
        PortfolioException.class,
        () -> validationHelper.validateRequest(TEST_USER_ID, List.of(), null, null, null));
    assertThrows(
        PortfolioException.class,
        () -> validationHelper.validateRequest(TEST_USER_ID, List.of(0L), null, null, null));
    assertThrows(
        PortfolioException.class,
        () ->
            validationHelper.validateRequest(
                TEST_USER_ID, Arrays.asList(TEST_PORTFOLIO_ID, null), null, null, null));
  }

  @Test
  void enforcesSelectionLimitsForPortfoliosAndAssets() {
    List<Long> portfolioIds =
        java.util.stream.LongStream.rangeClosed(1, TEST_MAX_PORTFOLIOS_PER_USER + 1)
            .boxed()
            .toList();
    List<Long> assetIds =
        java.util.stream.LongStream.rangeClosed(1, TEST_MAX_ASSETS_PER_PORTFOLIO + 1)
            .boxed()
            .toList();

    PortfolioException portfolioException =
        assertThrows(
            PortfolioException.class,
            () -> validationHelper.validateRequest(TEST_USER_ID, portfolioIds, null, null, null));
    PortfolioException assetException =
        assertThrows(
            PortfolioException.class,
            () -> validationHelper.validateRequest(TEST_USER_ID, null, assetIds, null, null));

    assertEquals(
        ErrorDefinition.PORTFOLIO_MARKET_DATA_SELECTION_LIMIT_EXCEEDED,
        portfolioException.getErrorDefinition());
    assertEquals(
        ErrorDefinition.PORTFOLIO_MARKET_DATA_SELECTION_LIMIT_EXCEEDED,
        assetException.getErrorDefinition());
  }

  @Test
  void rejectsReversedTimeRange() {
    PortfolioException exception =
        assertThrows(
            PortfolioException.class,
            () ->
                validationHelper.validateRequest(
                    TEST_USER_ID,
                    null,
                    List.of(TEST_ASSET_ID),
                    Instant.parse("2026-11-02T00:00:00Z"),
                    Instant.parse("2026-11-01T00:00:00Z")));

    assertEquals(
        ErrorDefinition.PORTFOLIO_MARKET_DATA_TIME_RANGE_INVALID, exception.getErrorDefinition());
  }

  @Test
  void rejectsPortfolioIdsNotResolvedForTheUser() {
    PortfolioEntity resolvedPortfolio = new PortfolioEntity();
    resolvedPortfolio.setId(TEST_PORTFOLIO_ID);

    PortfolioException exception =
        assertThrows(
            PortfolioException.class,
            () ->
                validationHelper.validatePortfoliosResolved(
                    List.of(TEST_PORTFOLIO_ID, TEST_PORTFOLIO_ID + 1), List.of(resolvedPortfolio)));

    assertEquals(ErrorDefinition.PORTFOLIO_NOT_FOUND, exception.getErrorDefinition());
    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
  }

  @Test
  void rejectsAssetIdsNotResolvedForTheUser() {
    AssetEntity resolvedAsset = new AssetEntity();
    resolvedAsset.setId(TEST_ASSET_ID);

    PortfolioException exception =
        assertThrows(
            PortfolioException.class,
            () ->
                validationHelper.validateAssetsResolved(
                    List.of(TEST_ASSET_ID, TEST_ASSET_ID + 1), List.of(resolvedAsset)));

    assertEquals(ErrorDefinition.ASSET_NOT_FOUND, exception.getErrorDefinition());
    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
  }
}

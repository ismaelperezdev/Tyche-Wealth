package com.tychewealth.service.helper.asset;

import static com.tychewealth.constants.TestConstants.TEST_ASSET_ID;
import static com.tychewealth.constants.TestConstants.TEST_ASSET_SYMBOL_MSFT;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_ID;
import static com.tychewealth.testdata.AiTestData.TEST_ASSET_NAME_MICROSOFT;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_NAME_APPLE;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_SYMBOL_AAPL;
import static com.tychewealth.testdata.AssetTestData.createRequestWithNameAndSymbol;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.asset.request.AssetCreateRequestDto;
import com.tychewealth.entity.AssetEntity;
import com.tychewealth.enums.AssetBatchActionEnum;
import com.tychewealth.error.exception.PortfolioException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.AssetRepository;
import java.util.Collections;
import java.util.List;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class AssetValidationHelperTest {

  @Mock private AssetRepository assetRepository;

  @Test
  void validateCreateLimitThrowsConflictWhenPortfolioAlreadyHasMaximumAssets() {
    AssetValidationHelper helper = new AssetValidationHelper(assetRepository);

    when(assetRepository.findByPortfolioId(TEST_PORTFOLIO_ID))
        .thenReturn(Collections.nCopies(200, new AssetEntity()));

    PortfolioException exception =
        assertThrows(PortfolioException.class, () -> helper.validateCreateLimit(TEST_PORTFOLIO_ID));

    assertEquals(ErrorDefinition.ASSET_LIMIT_REACHED, exception.getErrorDefinition());
    assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus());
  }

  @Test
  void validateCreateNameConflictThrowsConflictWhenAssetNameAlreadyExists() {
    AssetValidationHelper helper = new AssetValidationHelper(assetRepository);

    when(assetRepository.existsByPortfolioIdAndName(TEST_PORTFOLIO_ID, TEST_ASSET_NAME_APPLE))
        .thenReturn(true);

    PortfolioException exception =
        assertThrows(
            PortfolioException.class,
            () -> helper.validateCreateNameConflict(TEST_PORTFOLIO_ID, TEST_ASSET_NAME_APPLE));

    assertEquals(ErrorDefinition.ASSET_NAME_CONFLICT, exception.getErrorDefinition());
    assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus());
    assertEquals(TEST_ASSET_NAME_APPLE, exception.getDescription().get("name"));
  }

  @Test
  void validateBatchCreateRequestRequiresAction() {
    AssetValidationHelper helper = new AssetValidationHelper(assetRepository);

    PortfolioException exception =
        assertThrows(
            PortfolioException.class, () -> helper.validateBatchCreateRequest(null, null, null));

    assertEquals(ErrorDefinition.GENERIC_BAD_REQUEST, exception.getErrorDefinition());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }

  @Test
  void validateBatchCreateRequestForDiscardRequiresImportId() {
    AssetValidationHelper helper = new AssetValidationHelper(assetRepository);
    List<AssetCreateRequestDto> emptyAssets = Collections.emptyList();

    PortfolioException exception =
        assertThrows(
            PortfolioException.class,
            () ->
                helper.validateBatchCreateRequest(
                    AssetBatchActionEnum.DISCARD, "   ", emptyAssets));

    assertEquals(ErrorDefinition.GENERIC_BAD_REQUEST, exception.getErrorDefinition());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }

  @Test
  void validateBatchCreateRequestForCreateRequiresAssets() {
    AssetValidationHelper helper = new AssetValidationHelper(assetRepository);
    List<AssetCreateRequestDto> emptyAssets = Collections.emptyList();

    PortfolioException exception =
        assertThrows(
            PortfolioException.class,
            () ->
                helper.validateBatchCreateRequest(AssetBatchActionEnum.CREATE, null, emptyAssets));

    assertEquals(ErrorDefinition.GENERIC_BAD_REQUEST, exception.getErrorDefinition());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }

  @Test
  void validateBatchCreateLimitThrowsConflictWhenCombinedCountExceedsMaximum() {
    AssetValidationHelper helper = new AssetValidationHelper(assetRepository);

    when(assetRepository.findByPortfolioId(TEST_PORTFOLIO_ID))
        .thenReturn(Collections.nCopies(199, new AssetEntity()));

    PortfolioException exception =
        assertThrows(
            PortfolioException.class, () -> helper.validateBatchCreateLimit(TEST_PORTFOLIO_ID, 2));

    assertEquals(ErrorDefinition.ASSET_LIMIT_REACHED, exception.getErrorDefinition());
    assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus());
  }

  @Test
  void validateBatchCreateRequestDuplicatesThrowsConflictWhenSameNameAppearsTwice() {
    AssetValidationHelper helper = new AssetValidationHelper(assetRepository);
    AssetCreateRequestDto first =
        createRequestWithNameAndSymbol(TEST_ASSET_NAME_APPLE, TEST_ASSET_SYMBOL_AAPL);
    AssetCreateRequestDto second =
        createRequestWithNameAndSymbol("  " + TEST_ASSET_NAME_APPLE + "  ", "AAPL-2");
    List<AssetCreateRequestDto> assets = List.of(first, second);

    PortfolioException exception =
        assertThrows(
            PortfolioException.class, () -> helper.validateBatchCreateRequestDuplicates(assets));

    assertEquals(ErrorDefinition.ASSET_NAME_CONFLICT, exception.getErrorDefinition());
    assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus());
  }

  @Test
  void validateBatchCreateRequestDuplicatesAllowsDifferentNames() {
    AssetValidationHelper helper = new AssetValidationHelper(assetRepository);
    AssetCreateRequestDto first =
        createRequestWithNameAndSymbol(TEST_ASSET_NAME_APPLE, TEST_ASSET_SYMBOL_AAPL);
    AssetCreateRequestDto second =
        createRequestWithNameAndSymbol(TEST_ASSET_NAME_MICROSOFT, TEST_ASSET_SYMBOL_MSFT);

    helper.validateBatchCreateRequestDuplicates(List.of(first, second));
  }

  @Test
  void validateBatchCreateDatabaseConflictsThrowsNameConflictWhenNameExistsInPortfolio() {
    AssetValidationHelper helper = new AssetValidationHelper(assetRepository);
    AssetCreateRequestDto asset =
        createRequestWithNameAndSymbol(TEST_ASSET_NAME_APPLE, TEST_ASSET_SYMBOL_AAPL);
    List<AssetCreateRequestDto> assets = List.of(asset);

    when(assetRepository.existsByPortfolioIdAndName(TEST_PORTFOLIO_ID, TEST_ASSET_NAME_APPLE))
        .thenReturn(true);

    PortfolioException exception =
        assertThrows(
            PortfolioException.class,
            () -> helper.validateBatchCreateDatabaseConflicts(TEST_PORTFOLIO_ID, assets));

    assertEquals(ErrorDefinition.ASSET_NAME_CONFLICT, exception.getErrorDefinition());
    assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus());
    verify(assetRepository, never())
        .existsByPortfolioIdAndSymbol(TEST_PORTFOLIO_ID, TEST_ASSET_SYMBOL_AAPL);
  }

  @Test
  void validateBatchCreateDatabaseConflictsThrowsSymbolConflictWhenSymbolExistsInPortfolio() {
    AssetValidationHelper helper = new AssetValidationHelper(assetRepository);
    AssetCreateRequestDto asset =
        createRequestWithNameAndSymbol(TEST_ASSET_NAME_MICROSOFT, TEST_ASSET_SYMBOL_MSFT);
    List<AssetCreateRequestDto> assets = List.of(asset);

    when(assetRepository.existsByPortfolioIdAndName(TEST_PORTFOLIO_ID, TEST_ASSET_NAME_MICROSOFT))
        .thenReturn(false);
    when(assetRepository.existsByPortfolioIdAndSymbol(TEST_PORTFOLIO_ID, TEST_ASSET_SYMBOL_MSFT))
        .thenReturn(true);

    PortfolioException exception =
        assertThrows(
            PortfolioException.class,
            () -> helper.validateBatchCreateDatabaseConflicts(TEST_PORTFOLIO_ID, assets));

    assertEquals(ErrorDefinition.ASSET_SYMBOL_CONFLICT, exception.getErrorDefinition());
    assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus());
    assertEquals(TEST_ASSET_SYMBOL_MSFT, exception.getDescription().get("name"));
  }

  @Test
  void validateBatchCreateDatabaseConflictsAllowsAssetsWhenNoConflictsExist() {
    AssetValidationHelper helper = new AssetValidationHelper(assetRepository);
    AssetCreateRequestDto first =
        createRequestWithNameAndSymbol(TEST_ASSET_NAME_APPLE, TEST_ASSET_SYMBOL_AAPL);
    AssetCreateRequestDto second =
        createRequestWithNameAndSymbol(TEST_ASSET_NAME_MICROSOFT, TEST_ASSET_SYMBOL_MSFT);

    when(assetRepository.existsByPortfolioIdAndName(TEST_PORTFOLIO_ID, TEST_ASSET_NAME_APPLE))
        .thenReturn(false);
    when(assetRepository.existsByPortfolioIdAndSymbol(TEST_PORTFOLIO_ID, TEST_ASSET_SYMBOL_AAPL))
        .thenReturn(false);
    when(assetRepository.existsByPortfolioIdAndName(TEST_PORTFOLIO_ID, TEST_ASSET_NAME_MICROSOFT))
        .thenReturn(false);
    when(assetRepository.existsByPortfolioIdAndSymbol(TEST_PORTFOLIO_ID, TEST_ASSET_SYMBOL_MSFT))
        .thenReturn(false);

    helper.validateBatchCreateDatabaseConflicts(TEST_PORTFOLIO_ID, List.of(first, second));
  }

  @Test
  void translateSymbolPersistenceConflictReturnsPortfolioExceptionForUniqueConstraint() {
    AssetValidationHelper helper = new AssetValidationHelper(assetRepository);
    DataIntegrityViolationException exception =
        new DataIntegrityViolationException(
            "duplicate",
            new ConstraintViolationException(
                "could not execute statement", null, "uq_asset_portfolio_id_symbol_index"));

    PortfolioException result =
        helper.translateSymbolPersistenceConflict(
            exception, TEST_PORTFOLIO_ID, TEST_ASSET_SYMBOL_AAPL);

    assertEquals(ErrorDefinition.ASSET_SYMBOL_CONFLICT, result.getErrorDefinition());
    assertEquals(HttpStatus.CONFLICT, result.getHttpStatus());
    assertEquals(TEST_ASSET_SYMBOL_AAPL, result.getDescription().get("name"));
  }

  @Test
  void validateRetrievedAssetExistsReturnsAssetWhenItExistsInPortfolio() {
    AssetValidationHelper helper = new AssetValidationHelper(assetRepository);
    Long assetId = TEST_ASSET_ID;
    AssetEntity asset = new AssetEntity();
    asset.setId(assetId);

    when(assetRepository.findByIdAndPortfolioId(assetId, TEST_PORTFOLIO_ID))
        .thenReturn(java.util.Optional.of(asset));

    AssetEntity result = helper.validateRetrievedAssetExists(TEST_PORTFOLIO_ID, assetId);

    assertSame(asset, result);
  }

  @Test
  void validateRetrievedAssetExistsThrowsNotFoundWhenAssetDoesNotExistInPortfolio() {
    AssetValidationHelper helper = new AssetValidationHelper(assetRepository);
    Long assetId = TEST_ASSET_ID;

    when(assetRepository.findByIdAndPortfolioId(assetId, TEST_PORTFOLIO_ID))
        .thenReturn(java.util.Optional.empty());

    PortfolioException exception =
        assertThrows(
            PortfolioException.class,
            () -> helper.validateRetrievedAssetExists(TEST_PORTFOLIO_ID, assetId));

    assertEquals(ErrorDefinition.ASSET_NOT_FOUND, exception.getErrorDefinition());
    assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
  }

  @Test
  void translateSymbolPersistenceConflictRethrowsOriginalExceptionWhenConstraintIsUnrelated() {
    AssetValidationHelper helper = new AssetValidationHelper(assetRepository);
    DataIntegrityViolationException exception =
        new DataIntegrityViolationException("some unrelated persistence failure");

    DataIntegrityViolationException thrown =
        assertThrows(
            DataIntegrityViolationException.class,
            () ->
                helper.translateSymbolPersistenceConflict(
                    exception, TEST_PORTFOLIO_ID, TEST_ASSET_SYMBOL_AAPL));

    assertSame(exception, thrown);
  }
}

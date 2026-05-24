package com.tychewealth.service.helper.asset;

import static com.tychewealth.constants.TestConstants.TEST_ASSET_SYMBOL_MSFT;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_ID;
import static com.tychewealth.testdata.AiTestData.TEST_ASSET_NAME_MICROSOFT;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_NAME_APPLE;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_SYMBOL_AAPL;
import static com.tychewealth.testdata.AssetTestData.createRequestWithNameAndSymbol;
import static com.tychewealth.testdata.AssetTestData.defaultPortfolioEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.asset.AssetResponseDto;
import com.tychewealth.dto.asset.request.AssetCreateRequestDto;
import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.mapper.asset.AssetMapper;
import com.tychewealth.repository.AssetRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetCreateHelperTest {

  @Mock private AssetRepository assetRepository;
  @Mock private AssetMapper assetMapper;

  @Test
  void createMapsAssignsPortfolioPersistsAndReturnsMappedResponse() {
    AssetCreateHelper helper = new AssetCreateHelper(assetRepository, assetMapper);
    PortfolioEntity portfolio = defaultPortfolioEntity();
    portfolio.setId(TEST_PORTFOLIO_ID);
    AssetCreateRequestDto request =
        createRequestWithNameAndSymbol(TEST_ASSET_NAME_APPLE, TEST_ASSET_SYMBOL_AAPL);
    AssetEntity mappedAsset = new AssetEntity();
    AssetEntity persistedAsset = new AssetEntity();
    AssetResponseDto response = new AssetResponseDto();

    when(assetMapper.create(request)).thenReturn(mappedAsset);
    when(assetRepository.saveAndFlush(mappedAsset)).thenReturn(persistedAsset);
    when(assetMapper.toDto(persistedAsset)).thenReturn(response);

    AssetResponseDto result = helper.create(portfolio, request);

    assertSame(response, result);
    assertSame(portfolio, mappedAsset.getPortfolio());
    verify(assetMapper).create(request);
    verify(assetRepository).saveAndFlush(mappedAsset);
    verify(assetMapper).toDto(persistedAsset);
  }

  @Test
  void createBatchMapsAssignsPortfolioPersistsAndReturnsMappedResponses() {
    AssetCreateHelper helper = new AssetCreateHelper(assetRepository, assetMapper);
    PortfolioEntity portfolio = defaultPortfolioEntity();
    portfolio.setId(TEST_PORTFOLIO_ID);

    AssetCreateRequestDto firstRequest =
        createRequestWithNameAndSymbol(TEST_ASSET_NAME_APPLE, TEST_ASSET_SYMBOL_AAPL);
    AssetCreateRequestDto secondRequest =
        createRequestWithNameAndSymbol(TEST_ASSET_NAME_MICROSOFT, TEST_ASSET_SYMBOL_MSFT);

    AssetEntity firstMapped = new AssetEntity();
    AssetEntity secondMapped = new AssetEntity();
    AssetEntity firstPersisted = new AssetEntity();
    AssetEntity secondPersisted = new AssetEntity();
    AssetResponseDto firstResponse = new AssetResponseDto();
    AssetResponseDto secondResponse = new AssetResponseDto();

    when(assetMapper.create(firstRequest)).thenReturn(firstMapped);
    when(assetMapper.create(secondRequest)).thenReturn(secondMapped);
    when(assetRepository.saveAllAndFlush(List.of(firstMapped, secondMapped)))
        .thenReturn(List.of(firstPersisted, secondPersisted));
    when(assetMapper.toDto(firstPersisted)).thenReturn(firstResponse);
    when(assetMapper.toDto(secondPersisted)).thenReturn(secondResponse);

    List<AssetResponseDto> result =
        helper.createBatch(portfolio, List.of(firstRequest, secondRequest));

    assertEquals(2, result.size());
    assertSame(firstResponse, result.getFirst());
    assertSame(secondResponse, result.get(1));
    assertSame(portfolio, firstMapped.getPortfolio());
    assertSame(portfolio, secondMapped.getPortfolio());
    verify(assetMapper).create(firstRequest);
    verify(assetMapper).create(secondRequest);
    verify(assetRepository).saveAllAndFlush(List.of(firstMapped, secondMapped));
    verify(assetMapper).toDto(firstPersisted);
    verify(assetMapper).toDto(secondPersisted);
  }
}

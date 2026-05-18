package com.tychewealth.service.helper.asset;

import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_ID;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_AVERAGE_PRICE;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_NAME_APPLE;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_QUANTITY;
import static com.tychewealth.testdata.AssetTestData.TEST_ASSET_SYMBOL_AAPL;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.asset.AssetResponseDto;
import com.tychewealth.dto.asset.request.AssetCreateRequestDto;
import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.enums.InvestmentHorizonEnum;
import com.tychewealth.enums.RiskProfileEnum;
import com.tychewealth.enums.StrategyTypeEnum;
import com.tychewealth.mapper.asset.AssetMapper;
import com.tychewealth.repository.AssetRepository;
import com.tychewealth.testdata.EntityBuilder;
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
    PortfolioEntity portfolio =
        EntityBuilder.buildPortfolio(
            TEST_USER_ID,
            "Core",
            CurrencyCodeEnum.USD,
            RiskProfileEnum.MEDIUM,
            StrategyTypeEnum.BALANCED,
            InvestmentHorizonEnum.MEDIUM);
    portfolio.setId(TEST_PORTFOLIO_ID);
    AssetCreateRequestDto request =
        new AssetCreateRequestDto(
            TEST_ASSET_NAME_APPLE,
            TEST_ASSET_SYMBOL_AAPL,
            AssetTypeEnum.STOCK,
            TEST_ASSET_QUANTITY,
            TEST_ASSET_AVERAGE_PRICE,
            CurrencyCodeEnum.USD);
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
}

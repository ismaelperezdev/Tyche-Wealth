package com.tychewealth.repository;

import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testdata.EntityBuilder.buildAsset;
import static com.tychewealth.testdata.EntityBuilder.buildPortfolio;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.enums.InvestmentHorizonEnum;
import com.tychewealth.enums.RiskProfileEnum;
import com.tychewealth.enums.StrategyTypeEnum;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(
    properties = {"spring.liquibase.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class AssetRepositoryTest {

  @Autowired private AssetRepository assetRepository;

  @Autowired private PortfolioRepository portfolioRepository;

  private PortfolioEntity portfolio;

  @BeforeEach
  void setUp() {
    portfolio =
        portfolioRepository.save(
            buildPortfolio(
                TEST_USER_ID,
                "Asset Book",
                CurrencyCodeEnum.USD,
                RiskProfileEnum.MEDIUM,
                StrategyTypeEnum.BALANCED,
                InvestmentHorizonEnum.LONG));
  }

  @Test
  void findByPortfolioIdReturnsAssets() {
    assetRepository.save(
        buildAsset(portfolio, "Apple Inc.", "AAPL", AssetTypeEnum.STOCK, CurrencyCodeEnum.USD));

    List<AssetEntity> result = assetRepository.findByPortfolioId(portfolio.getId());

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("AAPL", result.get(0).getSymbol());
  }

  @Test
  void findByPortfolioIdAndSymbolReturnsAsset() {
    assetRepository.save(
        buildAsset(
            portfolio, "Microsoft Corporation", "MSFT", AssetTypeEnum.STOCK, CurrencyCodeEnum.USD));

    Optional<AssetEntity> result =
        assetRepository.findByPortfolioIdAndSymbol(portfolio.getId(), "MSFT");

    assertTrue(result.isPresent());
    assertEquals(AssetTypeEnum.STOCK, result.get().getAssetType());
  }

  @Test
  void findByCurrencyReturnsAssets() {
    assetRepository.save(
        buildAsset(portfolio, "Bitcoin", "BTC", AssetTypeEnum.CRYPTO, CurrencyCodeEnum.USD));

    List<AssetEntity> result = assetRepository.findByCurrency(CurrencyCodeEnum.USD);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("BTC", result.get(0).getSymbol());
  }

  @Test
  void findByAssetTypeReturnsAssets() {
    assetRepository.save(
        buildAsset(
            portfolio, "SPDR Gold Shares", "GLD", AssetTypeEnum.COMMODITY, CurrencyCodeEnum.USD));

    List<AssetEntity> result = assetRepository.findByAssetType(AssetTypeEnum.COMMODITY);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("GLD", result.get(0).getSymbol());
  }

  @Test
  void existsByPortfolioIdAndSymbolReturnsTrueWhenExists() {
    assetRepository.save(
        buildAsset(portfolio, "Tesla, Inc.", "TSLA", AssetTypeEnum.STOCK, CurrencyCodeEnum.USD));

    Boolean exists = assetRepository.existsByPortfolioIdAndSymbol(portfolio.getId(), "TSLA");

    assertEquals(Boolean.TRUE, exists);
  }

  @Test
  void existsByPortfolioIdAndNameReturnsTrueWhenExists() {
    assetRepository.save(
        buildAsset(portfolio, "Tesla, Inc.", "TSLA", AssetTypeEnum.STOCK, CurrencyCodeEnum.USD));

    Boolean exists = assetRepository.existsByPortfolioIdAndName(portfolio.getId(), "Tesla, Inc.");

    assertEquals(Boolean.TRUE, exists);
  }
}

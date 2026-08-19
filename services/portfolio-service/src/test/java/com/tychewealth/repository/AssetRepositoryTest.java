package com.tychewealth.repository;

import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testdata.EntityBuilder.buildAsset;
import static com.tychewealth.testdata.EntityBuilder.buildPortfolio;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.enums.InvestmentHorizonEnum;
import com.tychewealth.enums.RiskProfileEnum;
import com.tychewealth.enums.StrategyTypeEnum;
import java.time.LocalDateTime;
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
  void existsByPortfolioIdAndSymbolReturnsTrueWhenExists() {
    assetRepository.save(
        buildAsset(portfolio, "Tesla, Inc.", "TSLA", AssetTypeEnum.STOCK, CurrencyCodeEnum.USD));

    boolean exists = assetRepository.existsByPortfolioIdAndSymbol(portfolio.getId(), "TSLA");

    assertTrue(exists);
  }

  @Test
  void existsByPortfolioIdAndNameReturnsTrueWhenExists() {
    assetRepository.save(
        buildAsset(portfolio, "Tesla, Inc.", "TSLA", AssetTypeEnum.STOCK, CurrencyCodeEnum.USD));

    boolean exists = assetRepository.existsByPortfolioIdAndName(portfolio.getId(), "Tesla, Inc.");

    assertTrue(exists);
  }

  @Test
  void findDistinctSymbolsByUserIdsReturnsDistinctSymbolsForMatchingUsersOnly() {
    PortfolioEntity otherPortfolio =
        portfolioRepository.save(
            buildPortfolio(
                TEST_USER_ID + 1,
                "Other Asset Book",
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.LOW,
                StrategyTypeEnum.INDEX,
                InvestmentHorizonEnum.MEDIUM));

    assetRepository.save(
        buildAsset(portfolio, "Apple Inc.", "AAPL", AssetTypeEnum.STOCK, CurrencyCodeEnum.USD));
    assetRepository.save(
        buildAsset(portfolio, "Apple Again", "AAPL", AssetTypeEnum.ETF, CurrencyCodeEnum.USD));
    assetRepository.save(
        buildAsset(
            portfolio, "Microsoft Corporation", "MSFT", AssetTypeEnum.STOCK, CurrencyCodeEnum.USD));
    assetRepository.save(
        buildAsset(otherPortfolio, "Bitcoin", "BTC", AssetTypeEnum.CRYPTO, CurrencyCodeEnum.EUR));

    List<String> result = assetRepository.findDistinctSymbolsByUserIds(List.of(TEST_USER_ID));

    assertNotNull(result);
    assertEquals(2, result.size());
    assertTrue(result.contains("AAPL"));
    assertTrue(result.contains("MSFT"));
    assertFalse(result.contains("BTC"));
  }

  @Test
  void activeAssetQueriesExcludeSoftDeletedAssets() {
    AssetEntity deletedAsset =
        buildAsset(portfolio, "Deleted Asset", "DEL", AssetTypeEnum.STOCK, CurrencyCodeEnum.USD);
    deletedAsset.setDeletedAt(LocalDateTime.now());
    AssetEntity savedAsset = assetRepository.saveAndFlush(deletedAsset);

    assertTrue(
        assetRepository.findByIdAndPortfolioId(savedAsset.getId(), portfolio.getId()).isEmpty());
    assertTrue(assetRepository.findByPortfolioId(portfolio.getId()).isEmpty());
    assertFalse(assetRepository.existsByPortfolioIdAndName(portfolio.getId(), "Deleted Asset"));
    assertFalse(assetRepository.existsByPortfolioIdAndSymbol(portfolio.getId(), "DEL"));
    assertFalse(
        assetRepository.findDistinctSymbolsByUserIds(List.of(TEST_USER_ID)).contains("DEL"));
  }
}

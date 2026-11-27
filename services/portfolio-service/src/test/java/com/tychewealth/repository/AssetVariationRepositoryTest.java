package com.tychewealth.repository;

import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testdata.AssetVariationTestData.createdVariation;
import static com.tychewealth.testdata.AssetVariationTestData.deletedVariation;
import static com.tychewealth.testdata.AssetVariationTestData.manualUpdateVariation;
import static com.tychewealth.testdata.EntityBuilder.buildAsset;
import static com.tychewealth.testdata.EntityBuilder.buildPortfolio;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.AssetVariationEntity;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.AssetVariationTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.enums.InvestmentHorizonEnum;
import com.tychewealth.enums.RiskProfileEnum;
import com.tychewealth.enums.StrategyTypeEnum;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(
    properties = {
      "spring.liquibase.enabled=false",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.properties.hibernate.generate_statistics=true"
    })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class AssetVariationRepositoryTest {

  private static final LocalDateTime RANGE_START = LocalDateTime.of(2026, 1, 10, 0, 0);
  private static final LocalDateTime RANGE_END = LocalDateTime.of(2026, 1, 20, 0, 0);

  @Autowired private AssetVariationRepository assetVariationRepository;
  @Autowired private AssetRepository assetRepository;
  @Autowired private PortfolioRepository portfolioRepository;
  @Autowired private EntityManager entityManager;
  @Autowired private EntityManagerFactory entityManagerFactory;

  private PortfolioEntity portfolio;

  @BeforeEach
  void setUp() {
    portfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_USER_ID,
                "History Portfolio",
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.MEDIUM,
                StrategyTypeEnum.BALANCED,
                InvestmentHorizonEnum.LONG));
  }

  @Test
  void findsLatestStateBeforeRangeAndRangeVariationsIncludingSoftDeletedAssets() {
    AssetEntity deletedAsset = saveAsset("OLD", false);
    AssetEntity createdDuringRange = saveAsset("NEW", false);
    AssetEntity otherPortfolioAsset = saveAsset("OTHER", true);
    deletedAsset.setDeletedAt(RANGE_START.plusDays(5));
    assetRepository.saveAndFlush(deletedAsset);

    assetVariationRepository.saveAllAndFlush(
        List.of(
            createdVariation(deletedAsset, "10", "100", RANGE_START.minusDays(9)),
            manualUpdateVariation(deletedAsset, "10", "12", "100", "110", RANGE_START.minusDays(2)),
            deletedVariation(deletedAsset, "12", "110", RANGE_START.plusDays(5)),
            createdVariation(createdDuringRange, "3", "50", RANGE_START.plusDays(1)),
            manualUpdateVariation(createdDuringRange, "3", "4", "50", "55", RANGE_END.plusDays(1)),
            createdVariation(otherPortfolioAsset, "2", "75", RANGE_START.minusDays(1))));

    List<AssetVariationEntity> initialStates =
        assetVariationRepository.findLatestStatePerAssetAtOrBefore(portfolio.getId(), RANGE_START);
    List<AssetVariationEntity> rangeVariations =
        assetVariationRepository.findPortfolioVariationsAfterAndThrough(
            portfolio.getId(), RANGE_START, RANGE_END);

    assertEquals(1, initialStates.size());
    assertEquals(deletedAsset.getId(), initialStates.get(0).getAsset().getId());
    assertEquals(new BigDecimal("12"), initialStates.get(0).getNewQuantity());
    assertEquals(new BigDecimal("110"), initialStates.get(0).getNewAveragePrice());
    assertEquals(2, rangeVariations.size());
    assertTrue(
        rangeVariations.stream()
            .allMatch(v -> v.getAsset().getPortfolio().getId().equals(portfolio.getId())));
    assertTrue(
        rangeVariations.stream()
            .anyMatch(v -> v.getChangeType() == AssetVariationTypeEnum.DELETED));
    assertTrue(
        rangeVariations.stream().anyMatch(v -> v.getAsset().getId().equals(deletedAsset.getId())));
    assertTrue(
        rangeVariations.stream()
            .anyMatch(v -> v.getAsset().getId().equals(createdDuringRange.getId())));
  }

  @Test
  void fetchesAssetsWithVariationsWithoutAdditionalQueriesPerAsset() {
    AssetEntity firstAsset = saveAsset("FIRST", false);
    AssetEntity secondAsset = saveAsset("SECOND", false);
    assetVariationRepository.saveAllAndFlush(
        List.of(
            createdVariation(firstAsset, "10", "100", RANGE_START.minusDays(1)),
            createdVariation(secondAsset, "20", "200", RANGE_START.minusDays(1)),
            manualUpdateVariation(firstAsset, "10", "11", "100", "110", RANGE_START.plusDays(1)),
            manualUpdateVariation(secondAsset, "20", "21", "200", "210", RANGE_START.plusDays(1))));
    entityManager.clear();
    Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.clear();

    List<AssetVariationEntity> initialStates =
        assetVariationRepository.findLatestStatePerAssetAtOrBefore(portfolio.getId(), RANGE_START);
    List<AssetVariationEntity> rangeVariations =
        assetVariationRepository.findPortfolioVariationsAfterAndThrough(
            portfolio.getId(), RANGE_START, RANGE_END);

    assertEquals(2, initialStates.size());
    assertEquals(2, rangeVariations.size());
    assertTrue(initialStates.stream().allMatch(v -> v.getAsset().getSymbol() != null));
    assertTrue(rangeVariations.stream().allMatch(v -> v.getAsset().getSymbol() != null));
    assertEquals(2, statistics.getPrepareStatementCount());
  }

  private AssetEntity saveAsset(String symbol, boolean otherPortfolio) {
    PortfolioEntity owner = portfolio;
    if (otherPortfolio) {
      owner =
          portfolioRepository.saveAndFlush(
              buildPortfolio(
                  TEST_USER_ID + 1,
                  "Other Portfolio",
                  CurrencyCodeEnum.EUR,
                  RiskProfileEnum.MEDIUM,
                  StrategyTypeEnum.BALANCED,
                  InvestmentHorizonEnum.LONG));
    }
    return assetRepository.saveAndFlush(
        buildAsset(owner, symbol + " Asset", symbol, AssetTypeEnum.STOCK, CurrencyCodeEnum.EUR));
  }
}

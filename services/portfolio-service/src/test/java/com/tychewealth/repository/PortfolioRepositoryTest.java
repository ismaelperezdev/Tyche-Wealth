package com.tychewealth.repository;

import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_NAME_CORE;
import static com.tychewealth.constants.TestConstants.TEST_PORTFOLIO_NAME_RETIREMENT;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testdata.EntityBuilder.buildPortfolio;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.enums.InvestmentHorizonEnum;
import com.tychewealth.enums.RiskProfileEnum;
import com.tychewealth.enums.StrategyTypeEnum;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Pageable;

@DataJpaTest(
    properties = {"spring.liquibase.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class PortfolioRepositoryTest {

  @Autowired private PortfolioRepository portfolioRepository;

  @Test
  void findByUserIdOrderByCreatedAtAscReturnsPortfoliosOrderedByCreatedAt() {
    PortfolioEntity retirementPortfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_USER_ID,
                TEST_PORTFOLIO_NAME_RETIREMENT,
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.MEDIUM,
                StrategyTypeEnum.BALANCED,
                InvestmentHorizonEnum.MEDIUM));
    PortfolioEntity corePortfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_USER_ID,
                TEST_PORTFOLIO_NAME_CORE,
                CurrencyCodeEnum.USD,
                RiskProfileEnum.LOW,
                StrategyTypeEnum.INCOME,
                InvestmentHorizonEnum.LONG));

    retirementPortfolio.setCreatedAt(LocalDateTime.now());
    corePortfolio.setCreatedAt(LocalDateTime.now().minusDays(1));
    portfolioRepository.saveAndFlush(retirementPortfolio);
    portfolioRepository.saveAndFlush(corePortfolio);

    List<PortfolioEntity> result =
        portfolioRepository.findByUserIdOrderByCreatedAtAsc(TEST_USER_ID);

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals(TEST_PORTFOLIO_NAME_CORE, result.get(0).getName());
    assertEquals(TEST_PORTFOLIO_NAME_RETIREMENT, result.get(1).getName());
    assertTrue(result.get(0).getCreatedAt().isBefore(result.get(1).getCreatedAt()));
  }

  @Test
  void findByUserIdAndNameReturnsPortfolio() {
    portfolioRepository.save(
        buildPortfolio(
            TEST_USER_ID,
            "Growth",
            CurrencyCodeEnum.USD,
            RiskProfileEnum.HIGH,
            StrategyTypeEnum.GROWTH,
            InvestmentHorizonEnum.LONG));

    Optional<PortfolioEntity> result =
        portfolioRepository.findByUserIdAndName(TEST_USER_ID, "Growth");

    assertTrue(result.isPresent());
    assertEquals(CurrencyCodeEnum.USD, result.get().getBaseCurrency());
  }

  @Test
  void existsByUserIdAndNameReturnsTrueWhenExists() {
    portfolioRepository.save(
        buildPortfolio(
            TEST_USER_ID,
            "Retiro",
            CurrencyCodeEnum.EUR,
            RiskProfileEnum.LOW,
            StrategyTypeEnum.BALANCED,
            InvestmentHorizonEnum.LONG));

    Boolean exists = portfolioRepository.existsByUserIdAndName(TEST_USER_ID, "Retiro");

    assertEquals(Boolean.TRUE, exists);
  }

  @Test
  void activePortfolioQueriesExcludeSoftDeletedPortfolios() {
    PortfolioEntity deletedPortfolio =
        buildPortfolio(
            TEST_USER_ID,
            "Deleted",
            CurrencyCodeEnum.EUR,
            RiskProfileEnum.LOW,
            StrategyTypeEnum.BALANCED,
            InvestmentHorizonEnum.LONG);
    deletedPortfolio.setDeletedAt(LocalDateTime.now());
    PortfolioEntity savedPortfolio = portfolioRepository.saveAndFlush(deletedPortfolio);

    assertTrue(
        portfolioRepository.findByIdAndUserId(savedPortfolio.getId(), TEST_USER_ID).isEmpty());
    assertTrue(portfolioRepository.findByUserId(TEST_USER_ID, Pageable.unpaged()).isEmpty());
    assertEquals(0, portfolioRepository.countByUserId(TEST_USER_ID));
    assertFalse(portfolioRepository.existsByUserIdAndName(TEST_USER_ID, "Deleted"));
  }
}

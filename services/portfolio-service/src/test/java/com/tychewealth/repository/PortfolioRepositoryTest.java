package com.tychewealth.repository;

import static com.tychewealth.constants.TestConstants.TEST_OTHER_USER_ID;
import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static com.tychewealth.testdata.EntityBuilder.buildPortfolio;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
                "Retirement",
                CurrencyCodeEnum.EUR,
                RiskProfileEnum.MEDIUM,
                StrategyTypeEnum.BALANCED,
                InvestmentHorizonEnum.MEDIUM));
    PortfolioEntity corePortfolio =
        portfolioRepository.saveAndFlush(
            buildPortfolio(
                TEST_USER_ID,
                "Core",
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
    assertEquals("Core", result.get(0).getName());
    assertEquals("Retirement", result.get(1).getName());
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
  void findByBaseCurrencyReturnsPortfolio() {
    portfolioRepository.save(
        buildPortfolio(
            TEST_USER_ID,
            "Income",
            CurrencyCodeEnum.CHF,
            RiskProfileEnum.LOW,
            StrategyTypeEnum.INCOME,
            InvestmentHorizonEnum.LONG));

    List<PortfolioEntity> result = portfolioRepository.findByBaseCurrency(CurrencyCodeEnum.CHF);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("Income", result.get(0).getName());
  }

  @Test
  void findByRiskProfileReturnsPortfolio() {
    portfolioRepository.save(
        buildPortfolio(
            TEST_USER_ID,
            "Spec",
            CurrencyCodeEnum.USD,
            RiskProfileEnum.HIGH,
            StrategyTypeEnum.SPECULATIVE,
            InvestmentHorizonEnum.SHORT));

    List<PortfolioEntity> result = portfolioRepository.findByRiskProfile(RiskProfileEnum.HIGH);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("Spec", result.getFirst().getName());
  }

  @Test
  void findByStrategyTypeReturnsPortfolio() {
    portfolioRepository.save(
        buildPortfolio(
            TEST_USER_ID,
            "Div",
            CurrencyCodeEnum.EUR,
            RiskProfileEnum.MEDIUM,
            StrategyTypeEnum.DIVIDEND,
            InvestmentHorizonEnum.MEDIUM));

    List<PortfolioEntity> result =
        portfolioRepository.findByStrategyType(StrategyTypeEnum.DIVIDEND);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("Div", result.get(0).getName());
  }

  @Test
  void findByInvestmentHorizonReturnsPortfolio() {
    portfolioRepository.save(
        buildPortfolio(
            TEST_OTHER_USER_ID,
            "Long",
            CurrencyCodeEnum.GBP,
            RiskProfileEnum.MEDIUM,
            StrategyTypeEnum.VALUE,
            InvestmentHorizonEnum.LONG));

    List<PortfolioEntity> result =
        portfolioRepository.findByInvestmentHorizon(InvestmentHorizonEnum.LONG);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("Long", result.get(0).getName());
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
}

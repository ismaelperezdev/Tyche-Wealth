package com.tychewealth.repository;

import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.enums.InvestmentHorizonEnum;
import com.tychewealth.enums.RiskProfileEnum;
import com.tychewealth.enums.StrategyTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class PortfolioRepositoryTest {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity user = new UserEntity();

    @BeforeEach
    void setUp() {
        user = userRepository.save(buildUser("maria@tyche.com", "maria"));
    }

    @Test
    void findByUserIdReturnsPortfolio() {
        portfolioRepository.save(buildPortfolio(user, "Core", CurrencyCodeEnum.EUR, RiskProfileEnum.MEDIUM, StrategyTypeEnum.BALANCED, InvestmentHorizonEnum.MEDIUM));

        List<PortfolioEntity> result = portfolioRepository.findByUserId(user.getId());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Core", result.get(0).getName());
    }

    @Test
    void findByUserIdAndNameReturnsPortfolio() {
        portfolioRepository.save(buildPortfolio(user, "Growth", CurrencyCodeEnum.USD, RiskProfileEnum.HIGH, StrategyTypeEnum.GROWTH, InvestmentHorizonEnum.LONG));

        Optional<PortfolioEntity> result = portfolioRepository.findByUserIdAndName(user.getId(), "Growth");

        assertTrue(result.isPresent());
        assertEquals(CurrencyCodeEnum.USD, result.get().getBaseCurrency());
    }

    @Test
    void findByBaseCurrencyReturnsPortfolio() {
        portfolioRepository.save(buildPortfolio(user, "Income", CurrencyCodeEnum.CHF, RiskProfileEnum.LOW, StrategyTypeEnum.INCOME, InvestmentHorizonEnum.LONG));

        List<PortfolioEntity> result = portfolioRepository.findByBaseCurrency(CurrencyCodeEnum.CHF);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Income", result.get(0).getName());
    }

    @Test
    void findByRiskProfileReturnsPortfolio() {
        portfolioRepository.save(buildPortfolio(user, "Spec", CurrencyCodeEnum.USD, RiskProfileEnum.HIGH, StrategyTypeEnum.SPECULATIVE, InvestmentHorizonEnum.SHORT));

        List<PortfolioEntity> result = portfolioRepository.findByRiskProfile(RiskProfileEnum.HIGH);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Spec", result.get(0).getName());
    }

    @Test
    void findByStrategyTypeReturnsPortfolio() {
        portfolioRepository.save(buildPortfolio(user, "Div", CurrencyCodeEnum.EUR, RiskProfileEnum.MEDIUM, StrategyTypeEnum.DIVIDEND, InvestmentHorizonEnum.MEDIUM));

        List<PortfolioEntity> result = portfolioRepository.findByStrategyType(StrategyTypeEnum.DIVIDEND);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Div", result.get(0).getName());
    }

    @Test
    void findByInvestmentHorizonReturnsPortfolio() {
        portfolioRepository.save(buildPortfolio(user, "Long", CurrencyCodeEnum.GBP, RiskProfileEnum.MEDIUM, StrategyTypeEnum.VALUE, InvestmentHorizonEnum.LONG));

        List<PortfolioEntity> result = portfolioRepository.findByInvestmentHorizon(InvestmentHorizonEnum.LONG);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Long", result.get(0).getName());
    }

    @Test
    void existsByUserIdAndNameReturnsTrueWhenExists() {
        portfolioRepository.save(buildPortfolio(user, "Retiro", CurrencyCodeEnum.EUR, RiskProfileEnum.LOW, StrategyTypeEnum.BALANCED, InvestmentHorizonEnum.LONG));

        Boolean exists = portfolioRepository.existsByUserIdAndName(user.getId(), "Retiro");

        assertEquals(Boolean.TRUE, exists);
    }

    private UserEntity buildUser(String email, String username) {
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword("secret123");
        return user;
    }

    private PortfolioEntity buildPortfolio(
            UserEntity user,
            String name,
            CurrencyCodeEnum currency,
            RiskProfileEnum riskProfile,
            StrategyTypeEnum strategyType,
            InvestmentHorizonEnum investmentHorizon
    ) {
        PortfolioEntity portfolio = new PortfolioEntity();
        portfolio.setUser(user);
        portfolio.setName(name);
        portfolio.setDescription(name + " description");
        portfolio.setBaseCurrency(currency);
        portfolio.setRiskProfile(riskProfile);
        portfolio.setInvestmentHorizon(investmentHorizon);
        portfolio.setStrategyType(strategyType);
        portfolio.setMaxRisk(new BigDecimal("0.40"));
        return portfolio;
    }
}

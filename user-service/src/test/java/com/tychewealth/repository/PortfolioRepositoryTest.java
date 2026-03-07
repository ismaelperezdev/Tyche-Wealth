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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

        PortfolioEntity result = portfolioRepository.findByUserId(user.getId());

        assertNotNull(result);
        assertEquals("Core", result.getName());
    }

    @Test
    void findByUserIdAndNameReturnsPortfolio() {
        portfolioRepository.save(buildPortfolio(user, "Growth", CurrencyCodeEnum.USD, RiskProfileEnum.HIGH, StrategyTypeEnum.GROWTH, InvestmentHorizonEnum.LONG));

        PortfolioEntity result = portfolioRepository.findByUserIdAndName(user.getId(), "Growth");

        assertNotNull(result);
        assertEquals(CurrencyCodeEnum.USD, result.getBaseCurrency());
    }

    @Test
    void findByBaseCurrencyReturnsPortfolio() {
        portfolioRepository.save(buildPortfolio(user, "Income", CurrencyCodeEnum.CHF, RiskProfileEnum.LOW, StrategyTypeEnum.INCOME, InvestmentHorizonEnum.LONG));

        PortfolioEntity result = portfolioRepository.findByBaseCurrency(CurrencyCodeEnum.CHF);

        assertNotNull(result);
        assertEquals("Income", result.getName());
    }

    @Test
    void findByRiskProfileReturnsPortfolio() {
        portfolioRepository.save(buildPortfolio(user, "Spec", CurrencyCodeEnum.USD, RiskProfileEnum.HIGH, StrategyTypeEnum.SPECULATIVE, InvestmentHorizonEnum.SHORT));

        PortfolioEntity result = portfolioRepository.findByRiskProfile(RiskProfileEnum.HIGH);

        assertNotNull(result);
        assertEquals("Spec", result.getName());
    }

    @Test
    void findByStrategyTypeReturnsPortfolio() {
        portfolioRepository.save(buildPortfolio(user, "Div", CurrencyCodeEnum.EUR, RiskProfileEnum.MEDIUM, StrategyTypeEnum.DIVIDEND, InvestmentHorizonEnum.MEDIUM));

        PortfolioEntity result = portfolioRepository.findByStrategyType(StrategyTypeEnum.DIVIDEND);

        assertNotNull(result);
        assertEquals("Div", result.getName());
    }

    @Test
    void findByInvestmentHorizonReturnsPortfolio() {
        portfolioRepository.save(buildPortfolio(user, "Long", CurrencyCodeEnum.GBP, RiskProfileEnum.MEDIUM, StrategyTypeEnum.VALUE, InvestmentHorizonEnum.LONG));

        PortfolioEntity result = portfolioRepository.findByInvestmentHorizon(InvestmentHorizonEnum.LONG);

        assertNotNull(result);
        assertEquals("Long", result.getName());
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

package com.tychewealth.repository;

import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.AssetTypeEnum;
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
class AssetRepositoryTest {

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private UserRepository userRepository;

    private PortfolioEntity portfolio;

    @BeforeEach
    void setUp() {
        UserEntity user = userRepository.save(buildUser("asset-owner@tyche.com", "assetowner"));
        portfolio = portfolioRepository.save(buildPortfolio(
                user,
                "Asset Book",
                CurrencyCodeEnum.USD,
                RiskProfileEnum.MEDIUM,
                StrategyTypeEnum.BALANCED,
                InvestmentHorizonEnum.LONG
        ));
    }

    @Test
    void findByPortfolioIdReturnsAssets() {
        assetRepository.save(buildAsset(portfolio, "AAPL", AssetTypeEnum.STOCK, CurrencyCodeEnum.USD));

        List<AssetEntity> result = assetRepository.findByPortfolioId(portfolio.getId());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("AAPL", result.get(0).getSymbol());
    }

    @Test
    void findByPortfolioIdAndSymbolReturnsAsset() {
        assetRepository.save(buildAsset(portfolio, "MSFT", AssetTypeEnum.STOCK, CurrencyCodeEnum.USD));

        Optional<AssetEntity> result = assetRepository.findByPortfolioIdAndSymbol(portfolio.getId(), "MSFT");

        assertTrue(result.isPresent());
        assertEquals(AssetTypeEnum.STOCK, result.get().getAssetType());
    }

    @Test
    void findByCurrencyReturnsAssets() {
        assetRepository.save(buildAsset(portfolio, "BTC", AssetTypeEnum.CRYPTO, CurrencyCodeEnum.USD));

        List<AssetEntity> result = assetRepository.findByCurrency(CurrencyCodeEnum.USD);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("BTC", result.get(0).getSymbol());
    }

    @Test
    void findByAssetTypeReturnsAssets() {
        assetRepository.save(buildAsset(portfolio, "GLD", AssetTypeEnum.COMMODITY, CurrencyCodeEnum.USD));

        List<AssetEntity> result = assetRepository.findByAssetType(AssetTypeEnum.COMMODITY);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("GLD", result.get(0).getSymbol());
    }

    @Test
    void existsByPortfolioIdAndSymbolReturnsTrueWhenExists() {
        assetRepository.save(buildAsset(portfolio, "TSLA", AssetTypeEnum.STOCK, CurrencyCodeEnum.USD));

        Boolean exists = assetRepository.existsByPortfolioIdAndSymbol(portfolio.getId(), "TSLA");

        assertEquals(Boolean.TRUE, exists);
    }

    private UserEntity buildUser(String email, String username) {
        UserEntity user = new UserEntity();
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
        PortfolioEntity item = new PortfolioEntity();
        item.setUser(user);
        item.setName(name);
        item.setDescription(name + " description");
        item.setBaseCurrency(currency);
        item.setRiskProfile(riskProfile);
        item.setInvestmentHorizon(investmentHorizon);
        item.setStrategyType(strategyType);
        item.setMaxRisk(new BigDecimal("0.40"));
        return item;
    }

    private AssetEntity buildAsset(
            PortfolioEntity portfolio,
            String symbol,
            AssetTypeEnum assetType,
            CurrencyCodeEnum currency
    ) {
        AssetEntity item = new AssetEntity();
        item.setPortfolio(portfolio);
        item.setSymbol(symbol);
        item.setAssetType(assetType);
        item.setQuantity(new BigDecimal("10.00000000"));
        item.setAveragePrice(new BigDecimal("123.4567"));
        item.setCurrency(currency);
        return item;
    }
}

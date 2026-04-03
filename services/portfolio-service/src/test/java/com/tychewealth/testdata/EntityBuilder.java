package com.tychewealth.testdata;

import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.enums.InvestmentHorizonEnum;
import com.tychewealth.enums.RiskProfileEnum;
import com.tychewealth.enums.StrategyTypeEnum;
import java.math.BigDecimal;

public final class EntityBuilder {

  private EntityBuilder() {}

  public static PortfolioEntity buildPortfolio(
      Long userId,
      String name,
      CurrencyCodeEnum currency,
      RiskProfileEnum riskProfile,
      StrategyTypeEnum strategyType,
      InvestmentHorizonEnum investmentHorizon) {
    PortfolioEntity portfolio = new PortfolioEntity();
    portfolio.setUserId(userId);
    portfolio.setName(name);
    portfolio.setDescription(name + " description");
    portfolio.setBaseCurrency(currency);
    portfolio.setRiskProfile(riskProfile);
    portfolio.setInvestmentHorizon(investmentHorizon);
    portfolio.setStrategyType(strategyType);
    portfolio.setMaxRisk(new BigDecimal("0.40"));
    return portfolio;
  }

  public static AssetEntity buildAsset(
      PortfolioEntity portfolio,
      String name,
      String symbol,
      AssetTypeEnum assetType,
      CurrencyCodeEnum currency) {
    AssetEntity asset = new AssetEntity();
    asset.setPortfolio(portfolio);
    asset.setName(name);
    asset.setSymbol(symbol);
    asset.setAssetType(assetType);
    asset.setQuantity(new BigDecimal("10.00000000"));
    asset.setAveragePrice(new BigDecimal("123.4567"));
    asset.setCurrency(currency);
    return asset;
  }
}

package com.tychewealth.testdata;

import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.entity.RefreshTokenEntity;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.enums.InvestmentHorizonEnum;
import com.tychewealth.enums.RiskProfileEnum;
import com.tychewealth.enums.StrategyTypeEnum;
import java.math.BigDecimal;
import java.time.Instant;

public final class EntityBuilder {

  private EntityBuilder() {}

  public static UserEntity buildUser(String email, String username, String password) {
    UserEntity user = new UserEntity();
    user.setEmail(email);
    user.setUsername(username);
    user.setPassword(password);
    return user;
  }

  public static PortfolioEntity buildPortfolio(
      UserEntity user,
      String name,
      CurrencyCodeEnum currency,
      RiskProfileEnum riskProfile,
      StrategyTypeEnum strategyType,
      InvestmentHorizonEnum investmentHorizon) {
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

  public static RefreshTokenEntity buildRefreshToken(
      String token, UserEntity user, Instant expiresAt, boolean revoked) {
    RefreshTokenEntity refreshToken = new RefreshTokenEntity();
    refreshToken.setToken(token);
    refreshToken.setUser(user);
    refreshToken.setExpiresAt(expiresAt);
    refreshToken.setRevoked(revoked);
    return refreshToken;
  }

  public static AssetEntity buildAsset(
      PortfolioEntity portfolio,
      String symbol,
      AssetTypeEnum assetType,
      CurrencyCodeEnum currency) {
    AssetEntity asset = new AssetEntity();
    asset.setPortfolio(portfolio);
    asset.setSymbol(symbol);
    asset.setAssetType(assetType);
    asset.setQuantity(new BigDecimal("10.00000000"));
    asset.setAveragePrice(new BigDecimal("123.4567"));
    asset.setCurrency(currency);
    return asset;
  }
}

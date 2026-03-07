package com.tychewealth.repository;

import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.enums.InvestmentHorizonEnum;
import com.tychewealth.enums.RiskProfileEnum;
import com.tychewealth.enums.StrategyTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortfolioRepository extends JpaRepository<PortfolioEntity, Long> {

    PortfolioEntity findByUserId(Long userId);

    PortfolioEntity findByUserIdAndName(Long userId, String name);

    PortfolioEntity findByBaseCurrency(CurrencyCodeEnum baseCurrency);

    PortfolioEntity findByRiskProfile(RiskProfileEnum riskProfile);

    PortfolioEntity findByStrategyType(StrategyTypeEnum strategyType);

    PortfolioEntity findByInvestmentHorizon(InvestmentHorizonEnum investmentHorizon);

    Boolean existsByUserIdAndName(Long userId, String name);

}

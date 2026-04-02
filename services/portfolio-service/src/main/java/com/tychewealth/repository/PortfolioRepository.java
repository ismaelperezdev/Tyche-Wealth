package com.tychewealth.repository;

import com.tychewealth.entity.PortfolioEntity;
import com.tychewealth.enums.CurrencyCodeEnum;
import com.tychewealth.enums.InvestmentHorizonEnum;
import com.tychewealth.enums.RiskProfileEnum;
import com.tychewealth.enums.StrategyTypeEnum;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortfolioRepository extends JpaRepository<PortfolioEntity, Long> {

  List<PortfolioEntity> findByUserId(Long userId);

  Optional<PortfolioEntity> findByUserIdAndName(Long userId, String name);

  List<PortfolioEntity> findByBaseCurrency(CurrencyCodeEnum baseCurrency);

  List<PortfolioEntity> findByRiskProfile(RiskProfileEnum riskProfile);

  List<PortfolioEntity> findByStrategyType(StrategyTypeEnum strategyType);

  List<PortfolioEntity> findByInvestmentHorizon(InvestmentHorizonEnum investmentHorizon);

  Boolean existsByUserIdAndName(Long userId, String name);
}

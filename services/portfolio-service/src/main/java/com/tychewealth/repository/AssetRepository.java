package com.tychewealth.repository;

import com.tychewealth.entity.AssetEntity;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetRepository extends JpaRepository<AssetEntity, Long> {

  List<AssetEntity> findByPortfolioId(Long portfolioId);

  Boolean existsByPortfolioIdAndName(Long portfolioId, String name);

  Optional<AssetEntity> findByPortfolioIdAndSymbol(Long portfolioId, String symbol);

  Optional<AssetEntity> findByIdAndPortfolioId(Long id, Long portfolioId);

  List<AssetEntity> findByCurrency(CurrencyCodeEnum currency);

  List<AssetEntity> findByAssetType(AssetTypeEnum assetType);

  Boolean existsByPortfolioIdAndSymbol(Long portfolioId, String symbol);
}

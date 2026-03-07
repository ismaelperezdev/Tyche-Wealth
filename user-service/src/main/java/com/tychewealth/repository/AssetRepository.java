package com.tychewealth.repository;

import com.tychewealth.entity.AssetEntity;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<AssetEntity, Long> {

    List<AssetEntity> findByPortfolioId(Long portfolioId);

    Optional<AssetEntity> findByPortfolioIdAndSymbol(Long portfolioId, String symbol);

    List<AssetEntity> findByCurrency(CurrencyCodeEnum currency);

    List<AssetEntity> findByAssetType(AssetTypeEnum assetType);

    Boolean existsByPortfolioIdAndSymbol(Long portfolioId, String symbol);
}

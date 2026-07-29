package com.tychewealth.repository;

import com.tychewealth.entity.AssetEntity;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetRepository extends JpaRepository<AssetEntity, Long> {

  List<AssetEntity> findByPortfolioId(Long portfolioId);

  Page<AssetEntity> findByPortfolioId(Long portfolioId, Pageable pageable);

  boolean existsByPortfolioIdAndName(Long portfolioId, String name);

  Optional<AssetEntity> findByPortfolioIdAndSymbol(Long portfolioId, String symbol);

  Optional<AssetEntity> findByIdAndPortfolioId(Long id, Long portfolioId);

  void deleteByIdAndPortfolioId(Long id, Long portfolioId);

  List<AssetEntity> findByCurrency(CurrencyCodeEnum currency);

  List<AssetEntity> findByAssetType(AssetTypeEnum assetType);

  boolean existsByPortfolioIdAndSymbol(Long portfolioId, String symbol);

  @Query(
      """
      select distinct a.symbol
      from AssetEntity a
      join a.portfolio p
      where p.userId in :userIds
        and a.symbol is not null
        and a.symbol <> ''
      """)
  List<String> findDistinctSymbolsByUserIds(@Param("userIds") Collection<Long> userIds);
}

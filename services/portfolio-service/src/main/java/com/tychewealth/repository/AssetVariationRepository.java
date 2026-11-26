package com.tychewealth.repository;

import com.tychewealth.entity.AssetVariationEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetVariationRepository extends JpaRepository<AssetVariationEntity, Long> {

  @Query(
      """
      select variation
      from AssetVariationEntity variation
      join fetch variation.asset asset
      where asset.portfolio.id = :portfolioId
        and variation.occurredAt <= :rangeStartInclusive
        and not exists (
          select newer.id
          from AssetVariationEntity newer
          where newer.asset.id = asset.id
            and newer.occurredAt <= :rangeStartInclusive
            and (newer.occurredAt > variation.occurredAt
              or (newer.occurredAt = variation.occurredAt and newer.id > variation.id))
        )
      order by asset.id
      """)
  List<AssetVariationEntity> findLatestStatePerAssetAtOrBefore(
      @Param("portfolioId") Long portfolioId,
      @Param("rangeStartInclusive") LocalDateTime rangeStartInclusive);

  @Query(
      """
       select variation
       from AssetVariationEntity variation
       join fetch variation.asset
       where variation.asset.portfolio.id = :portfolioId
        and variation.occurredAt > :rangeStartExclusive
        and variation.occurredAt <= :rangeEndInclusive
      order by variation.occurredAt, variation.id
      """)
  List<AssetVariationEntity> findPortfolioVariationsAfterAndThrough(
      @Param("portfolioId") Long portfolioId,
      @Param("rangeStartExclusive") LocalDateTime rangeStartExclusive,
      @Param("rangeEndInclusive") LocalDateTime rangeEndInclusive);
}

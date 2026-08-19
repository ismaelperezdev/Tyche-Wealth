package com.tychewealth.repository;

import com.tychewealth.entity.AssetEntity;
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

  @Query(
      """
      select a
      from AssetEntity a
      where a.portfolio.id = :portfolioId
        and a.deletedAt is null
      """)
  List<AssetEntity> findByPortfolioId(@Param("portfolioId") Long portfolioId);

  @Query(
      """
      select a
      from AssetEntity a
      where a.portfolio.id = :portfolioId
        and a.deletedAt is null
      """)
  Page<AssetEntity> findByPortfolioId(@Param("portfolioId") Long portfolioId, Pageable pageable);

  @Query(
      """
      select case when count(a) > 0 then true else false end
      from AssetEntity a
      where a.portfolio.id = :portfolioId
        and a.name = :name
        and a.deletedAt is null
      """)
  boolean existsByPortfolioIdAndName(
      @Param("portfolioId") Long portfolioId, @Param("name") String name);

  @Query(
      """
      select a
      from AssetEntity a
      where a.portfolio.id = :portfolioId
        and a.symbol = :symbol
        and a.deletedAt is null
      """)
  Optional<AssetEntity> findByPortfolioIdAndSymbol(
      @Param("portfolioId") Long portfolioId, @Param("symbol") String symbol);

  @Query(
      """
      select a
      from AssetEntity a
      where a.id = :id
        and a.portfolio.id = :portfolioId
        and a.deletedAt is null
      """)
  Optional<AssetEntity> findByIdAndPortfolioId(
      @Param("id") Long id, @Param("portfolioId") Long portfolioId);

  @Query(
      """
      select case when count(a) > 0 then true else false end
      from AssetEntity a
      where a.portfolio.id = :portfolioId
        and a.symbol = :symbol
        and a.deletedAt is null
      """)
  boolean existsByPortfolioIdAndSymbol(
      @Param("portfolioId") Long portfolioId, @Param("symbol") String symbol);

  @Query(
      """
      select distinct a.symbol
      from AssetEntity a
      join a.portfolio p
      where p.userId in :userIds
        and p.deletedAt is null
        and a.deletedAt is null
        and a.symbol is not null
        and a.symbol <> ''
      """)
  List<String> findDistinctSymbolsByUserIds(@Param("userIds") Collection<Long> userIds);
}

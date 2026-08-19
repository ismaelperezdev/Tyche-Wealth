package com.tychewealth.repository;

import com.tychewealth.entity.PortfolioEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PortfolioRepository extends JpaRepository<PortfolioEntity, Long> {

  @Query(
      """
      select p
      from PortfolioEntity p
      where p.userId = :userId
        and p.deletedAt is null
      order by p.createdAt asc
      """)
  List<PortfolioEntity> findByUserIdOrderByCreatedAtAsc(@Param("userId") Long userId);

  @Query(
      """
      select p
      from PortfolioEntity p
      where p.userId = :userId
        and p.deletedAt is null
      """)
  Page<PortfolioEntity> findByUserId(@Param("userId") Long userId, Pageable pageable);

  @Query(
      """
      select count(p)
      from PortfolioEntity p
      where p.userId = :userId
        and p.deletedAt is null
      """)
  long countByUserId(@Param("userId") Long userId);

  @Query(
      """
      select p
      from PortfolioEntity p
      where p.id = :id
        and p.userId = :userId
        and p.deletedAt is null
      """)
  Optional<PortfolioEntity> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

  @Query(
      """
      select p
      from PortfolioEntity p
      where p.userId = :userId
        and p.name = :name
        and p.deletedAt is null
      """)
  Optional<PortfolioEntity> findByUserIdAndName(
      @Param("userId") Long userId, @Param("name") String name);

  @Query(
      """
      select case when count(p) > 0 then true else false end
      from PortfolioEntity p
      where p.userId = :userId
        and p.name = :name
        and p.deletedAt is null
      """)
  boolean existsByUserIdAndName(@Param("userId") Long userId, @Param("name") String name);
}

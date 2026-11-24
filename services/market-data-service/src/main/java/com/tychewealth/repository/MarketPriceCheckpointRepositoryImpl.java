package com.tychewealth.repository;

import com.tychewealth.entity.MarketPriceCheckpointEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.domain.Specification;

/** Implements count-free checkpoint batch reads while eagerly loading each checkpoint's symbol. */
public class MarketPriceCheckpointRepositoryImpl {

  @PersistenceContext private EntityManager entityManager;

  public Slice<MarketPriceCheckpointEntity> findScanSlice(
      Specification<MarketPriceCheckpointEntity> specification, Pageable pageable) {
    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<MarketPriceCheckpointEntity> criteriaQuery =
        criteriaBuilder.createQuery(MarketPriceCheckpointEntity.class);
    Root<MarketPriceCheckpointEntity> root = criteriaQuery.from(MarketPriceCheckpointEntity.class);
    root.fetch("marketSymbol", JoinType.INNER);
    criteriaQuery.select(root).distinct(true);

    if (specification != null) {
      criteriaQuery.where(specification.toPredicate(root, criteriaQuery, criteriaBuilder));
    }
    criteriaQuery.orderBy(
        criteriaBuilder.asc(root.get("capturedAt")), criteriaBuilder.asc(root.get("id")));

    List<MarketPriceCheckpointEntity> batch =
        entityManager
            .createQuery(criteriaQuery)
            .setFirstResult(Math.toIntExact(pageable.getOffset()))
            .setMaxResults(pageable.getPageSize() + 1)
            .getResultList();
    boolean hasNext = batch.size() > pageable.getPageSize();
    if (hasNext) {
      batch.removeLast();
    }
    return new SliceImpl<>(batch, pageable, hasNext);
  }
}

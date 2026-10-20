package com.tychewealth.listing;

import com.tychewealth.listing.dto.Filterable;
import com.tychewealth.listing.exception.FilterException;
import com.tychewealth.listing.filter.FilterParser;
import com.tychewealth.listing.filter.ListQuery;
import com.tychewealth.listing.filter.ListQueryArgumentResolver;
import com.tychewealth.listing.jpa.JpaListingSpecificationBuilder;
import com.tychewealth.listing.shared.FilterFieldMapping;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Shared fixtures for listing-support integration tests. */
public final class ListingTestData {

  private ListingTestData() {}

  public static void insertSampleAssets(EntityManager entityManager) {
    entityManager.createQuery("delete from ListingAsset").executeUpdate();
    entityManager.persist(
        new ListingAsset(
            "AAPL", "Apple", "Technology", 3, LocalDate.parse("2023-01-10"), AssetStatus.ACTIVE));
    entityManager.persist(
        new ListingAsset(
            "MSFT",
            "Microsoft",
            "Technology",
            8,
            LocalDate.parse("2022-05-03"),
            AssetStatus.ACTIVE));
    entityManager.persist(
        new ListingAsset(
            "XOM", "Exxon", "Energy", 1, LocalDate.parse("2021-08-20"), AssetStatus.INACTIVE));
    entityManager.persist(new ListingAsset("BABA", "Alibaba", "Other", 2, null, null));
    entityManager.flush();
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @EntityScan(basePackageClasses = ListingAsset.class)
  @Import({
    ListingSupportConfiguration.class,
    ListQueryArgumentResolver.class,
    FilterParser.class,
    JpaListingSpecificationBuilder.class,
    ListingAssetsController.class,
    ListingTestExceptionHandler.class
  })
  static class TestApplication {}

  @RestController
  static class ListingAssetsController {

    private final EntityManager entityManager;
    private final JpaListingSpecificationBuilder specificationBuilder;

    ListingAssetsController(
        EntityManager entityManager, JpaListingSpecificationBuilder specificationBuilder) {
      this.entityManager = entityManager;
      this.specificationBuilder = specificationBuilder;
    }

    @Listable(mapping = AssetFilterMapping.class)
    @GetMapping("/integration/assets")
    List<AssetResponse> listAssets(ListQuery listQuery) {
      CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
      CriteriaQuery<ListingAsset> query = criteriaBuilder.createQuery(ListingAsset.class);
      Root<ListingAsset> root = query.from(ListingAsset.class);
      Specification<ListingAsset> specification =
          specificationBuilder.fromFilters(listQuery.filter(), new AssetFilterMapping());
      query
          .select(root)
          .where(specification.toPredicate(root, query, criteriaBuilder))
          .orderBy(
              listQuery.pageCriteria().sorts().stream()
                  .map(
                      sort ->
                          sort.direction().name().equals("DESC")
                              ? criteriaBuilder.desc(root.get(sort.field()))
                              : criteriaBuilder.asc(root.get(sort.field())))
                  .toList());

      return entityManager
          .createQuery(query)
          .setFirstResult(listQuery.pageCriteria().offset())
          .setMaxResults(listQuery.pageCriteria().limit())
          .getResultList()
          .stream()
          .map(
              asset ->
                  new AssetResponse(
                      asset.symbol,
                      asset.name,
                      new AssetDetails(asset.category),
                      asset.acquiredOn,
                      asset.status))
          .toList();
    }
  }

  @RestControllerAdvice
  static class ListingTestExceptionHandler {

    @ExceptionHandler(FilterException.class)
    ResponseEntity<Map<String, String>> handleFilterException(FilterException exception) {
      return ResponseEntity.status(exception.getHttpStatus())
          .body(Map.of("code", exception.getErrorDefinition().getCode()));
    }
  }

  public static class AssetFilterMapping extends FilterFieldMapping {
    @Override
    protected Map<String, String> fieldNameMap() {
      return Map.of(
          "symbol", "symbol",
          "ticker", "symbol",
          "name", "name",
          "category", "category",
          "details.category", "category",
          "quantity", "quantity",
          "acquiredOn", "acquiredOn",
          "status", "status");
    }

    @Override
    protected Map<String, Function<String, Object>> valueTransformers() {
      return Map.of("status", value -> "null".equalsIgnoreCase(value) ? null : value);
    }
  }

  @Filterable
  record AssetResponse(
      String symbol, String name, AssetDetails details, LocalDate acquiredOn, AssetStatus status) {}

  @Filterable
  record AssetDetails(String category) {}

  public enum AssetStatus {
    ACTIVE,
    INACTIVE
  }

  @Entity(name = "ListingAsset")
  public static class ListingAsset {

    @Id @GeneratedValue private Long id;

    private String symbol;
    private String name;
    private String category;
    Integer quantity;
    private LocalDate acquiredOn;
    private AssetStatus status;

    protected ListingAsset() {}

    public ListingAsset(
        String symbol,
        String name,
        String category,
        Integer quantity,
        LocalDate acquiredOn,
        AssetStatus status) {
      this.symbol = symbol;
      this.name = name;
      this.category = category;
      this.quantity = quantity;
      this.acquiredOn = acquiredOn;
      this.status = status;
    }

    public String getSymbol() {
      return symbol;
    }
  }
}

package com.tychewealth.listing;

import static com.tychewealth.listing.ListingTestConstants.ACQUIRED_ON_FIELD;
import static com.tychewealth.listing.ListingTestConstants.ASSET_AAPL;
import static com.tychewealth.listing.ListingTestConstants.ASSET_BABA;
import static com.tychewealth.listing.ListingTestConstants.ASSET_ENDPOINT;
import static com.tychewealth.listing.ListingTestConstants.ASSET_ENTITY_NAME;
import static com.tychewealth.listing.ListingTestConstants.ASSET_MSFT;
import static com.tychewealth.listing.ListingTestConstants.ASSET_XOM;
import static com.tychewealth.listing.ListingTestConstants.CATEGORY_ENERGY;
import static com.tychewealth.listing.ListingTestConstants.CATEGORY_FIELD;
import static com.tychewealth.listing.ListingTestConstants.CATEGORY_TECHNOLOGY;
import static com.tychewealth.listing.ListingTestConstants.CONTENT_RANGE_HEADER;
import static com.tychewealth.listing.ListingTestConstants.DATE_AAPL_ACQUIRED;
import static com.tychewealth.listing.ListingTestConstants.DETAILS_CATEGORY_FIELD;
import static com.tychewealth.listing.ListingTestConstants.NAME_ALIBABA;
import static com.tychewealth.listing.ListingTestConstants.NAME_APPLE;
import static com.tychewealth.listing.ListingTestConstants.NAME_EXXON;
import static com.tychewealth.listing.ListingTestConstants.NAME_FIELD;
import static com.tychewealth.listing.ListingTestConstants.NAME_MICROSOFT;
import static com.tychewealth.listing.ListingTestConstants.NULL_TOKEN;
import static com.tychewealth.listing.ListingTestConstants.QUANTITY_FIELD;
import static com.tychewealth.listing.ListingTestConstants.STATUS_FIELD;
import static com.tychewealth.listing.ListingTestConstants.SYMBOL_FIELD;
import static com.tychewealth.listing.ListingTestConstants.TICKER_FIELD;

import com.tychewealth.listing.dto.Filterable;
import com.tychewealth.listing.exception.FilterException;
import com.tychewealth.listing.filter.FieldFilterAdvice;
import com.tychewealth.listing.filter.FieldFiltered;
import com.tychewealth.listing.filter.FilterParser;
import com.tychewealth.listing.filter.ListQuery;
import com.tychewealth.listing.filter.ListQueryArgumentResolver;
import com.tychewealth.listing.filter.PaginationHttpHeaders;
import com.tychewealth.listing.jpa.JpaListingSpecificationBuilder;
import com.tychewealth.listing.shared.FilterFieldMapping;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.http.HttpServletRequest;
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
    entityManager.createQuery("delete from " + ASSET_ENTITY_NAME).executeUpdate();
    entityManager.persist(
        new ListingAsset(
            ASSET_AAPL,
            NAME_APPLE,
            CATEGORY_TECHNOLOGY,
            3,
            LocalDate.parse(DATE_AAPL_ACQUIRED),
            AssetStatus.ACTIVE));
    entityManager.persist(
        new ListingAsset(
            ASSET_MSFT,
            NAME_MICROSOFT,
            CATEGORY_TECHNOLOGY,
            8,
            LocalDate.parse("2022-05-03"),
            AssetStatus.ACTIVE));
    entityManager.persist(
        new ListingAsset(
            ASSET_XOM,
            NAME_EXXON,
            CATEGORY_ENERGY,
            1,
            LocalDate.parse("2021-08-20"),
            AssetStatus.INACTIVE));
    entityManager.persist(new ListingAsset(ASSET_BABA, NAME_ALIBABA, "Other", 2, null, null));
    entityManager.flush();
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @EntityScan(basePackageClasses = ListingAsset.class)
  @Import({
    ListingSupportConfiguration.class,
    ListQueryArgumentResolver.class,
    FilterParser.class,
    FieldFilterAdvice.class,
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
    @GetMapping(ASSET_ENDPOINT)
    ResponseEntity<List<AssetResponse>> listAssets(
        ListQuery listQuery, HttpServletRequest request) {
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

      CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
      Root<ListingAsset> countRoot = countQuery.from(ListingAsset.class);
      countQuery
          .select(criteriaBuilder.count(countRoot))
          .where(specification.toPredicate(countRoot, countQuery, criteriaBuilder));
      long total = entityManager.createQuery(countQuery).getSingleResult();
      var headers =
          PaginationHttpHeaders.build(
              request.getRequestURI(),
              listQuery.pageCriteria().offset(),
              listQuery.pageCriteria().limit(),
              total,
              request.getQueryString());
      if (listQuery.range() != null) {
        headers.set(
            CONTENT_RANGE_HEADER,
            listQuery.range().withTotalElements(total).toContentRangeHeader());
      }
      List<AssetResponse> body =
          entityManager
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
      return ResponseEntity.ok().headers(headers).body(body);
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
          SYMBOL_FIELD, SYMBOL_FIELD,
          TICKER_FIELD, SYMBOL_FIELD,
          NAME_FIELD, NAME_FIELD,
          CATEGORY_FIELD, CATEGORY_FIELD,
          DETAILS_CATEGORY_FIELD, CATEGORY_FIELD,
          QUANTITY_FIELD, QUANTITY_FIELD,
          ACQUIRED_ON_FIELD, ACQUIRED_ON_FIELD,
          STATUS_FIELD, STATUS_FIELD);
    }

    @Override
    protected Map<String, Function<String, Object>> valueTransformers() {
      return Map.of(STATUS_FIELD, value -> NULL_TOKEN.equalsIgnoreCase(value) ? null : value);
    }
  }

  @Filterable
  @FieldFiltered
  record AssetResponse(
      String symbol, String name, AssetDetails details, LocalDate acquiredOn, AssetStatus status) {}

  @Filterable
  record AssetDetails(String category) {}

  public enum AssetStatus {
    ACTIVE,
    INACTIVE
  }

  @Entity(name = ASSET_ENTITY_NAME)
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

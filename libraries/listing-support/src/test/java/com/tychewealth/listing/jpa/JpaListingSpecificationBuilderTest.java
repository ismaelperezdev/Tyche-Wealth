package com.tychewealth.listing.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tychewealth.listing.ListingTestData;
import com.tychewealth.listing.domain.ApiFilter;
import com.tychewealth.listing.enums.FilterOperator;
import com.tychewealth.listing.exception.FilterException;
import com.tychewealth.listing.shared.FilterFieldMapping;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

class JpaListingSpecificationBuilderTest {

  private StandardServiceRegistry registry;
  private SessionFactory sessionFactory;
  private final JpaListingSpecificationBuilder builder = new JpaListingSpecificationBuilder();
  private final FilterFieldMapping mapping = new ListingTestData.AssetFilterMapping();

  @BeforeEach
  void setUp() {
    registry =
        new StandardServiceRegistryBuilder()
            .applySetting("hibernate.connection.driver_class", "org.h2.Driver")
            .applySetting(
                "hibernate.connection.url", "jdbc:h2:mem:listing-support;DB_CLOSE_DELAY=-1")
            .applySetting("hibernate.connection.username", "sa")
            .applySetting("hibernate.connection.password", "")
            .applySetting("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
            .applySetting("hibernate.hbm2ddl.auto", "create-drop")
            .applySetting("hibernate.show_sql", "false")
            .build();
    sessionFactory =
        new MetadataSources(registry)
            .addAnnotatedClass(ListingTestData.ListingAsset.class)
            .buildMetadata()
            .buildSessionFactory();

    try (var session = sessionFactory.openSession()) {
      var transaction = session.beginTransaction();
      ListingTestData.insertSampleAssets(session);
      transaction.commit();
    }
  }

  @AfterEach
  void tearDown() {
    if (sessionFactory != null) {
      sessionFactory.close();
    }
    if (registry != null) {
      StandardServiceRegistryBuilder.destroy(registry);
    }
  }

  @Test
  void appliesAndCombinesTypedFilters() {
    List<ListingTestData.ListingAsset> result =
        find(
            List.of(
                new ApiFilter("ticker", FilterOperator.EQ, "AAPL"),
                new ApiFilter("quantity", FilterOperator.GTE, "2")));

    assertEquals(
        List.of("AAPL"), result.stream().map(ListingTestData.ListingAsset::getSymbol).toList());
  }

  @Test
  void treatsMultipleEqualityValuesAsAlternativesAndSupportsIn() {
    List<ListingTestData.ListingAsset> equalityResults =
        find(List.of(new ApiFilter("symbol", FilterOperator.EQ, "AAPL", "MSFT")));
    List<ListingTestData.ListingAsset> inResults =
        find(List.of(new ApiFilter("symbol", FilterOperator.IN, "AAPL", "MSFT")));

    assertEquals(2, equalityResults.size());
    assertEquals(2, inResults.size());
  }

  @Test
  void excludesEveryValueForNotEqualAndMatchesAnyValueForLike() {
    List<ListingTestData.ListingAsset> notEqualResults =
        find(List.of(new ApiFilter("symbol", FilterOperator.NEQ, "AAPL", "MSFT")));
    List<ListingTestData.ListingAsset> likeResults =
        find(List.of(new ApiFilter("name", FilterOperator.LIKE, "Apple", "Exxon")));

    assertEquals(List.of("BABA", "XOM"), symbols(notEqualResults).stream().sorted().toList());
    assertEquals(List.of("AAPL", "XOM"), symbols(likeResults).stream().sorted().toList());
  }

  @Test
  void returnsAllRowsForNoFiltersAndAppliesInclusiveAndExclusiveBoundaries() {
    List<ListingTestData.ListingAsset> noFilters = find(List.of());
    List<ListingTestData.ListingAsset> greaterThan =
        find(List.of(new ApiFilter("quantity", FilterOperator.GT, "3")));
    List<ListingTestData.ListingAsset> greaterThanOrEqual =
        find(List.of(new ApiFilter("quantity", FilterOperator.GTE, "3")));

    assertEquals(4, noFilters.size());
    assertEquals(List.of("MSFT"), symbols(greaterThan));
    assertEquals(List.of("AAPL", "MSFT"), symbols(greaterThanOrEqual).stream().sorted().toList());
  }

  @Test
  void supportsCaseInsensitiveSubstringFilters() {
    List<ListingTestData.ListingAsset> result =
        find(List.of(new ApiFilter("name", FilterOperator.LIKE, "APP")));

    assertEquals(
        List.of("AAPL"), result.stream().map(ListingTestData.ListingAsset::getSymbol).toList());
  }

  @Test
  void rejectsUnmappedFieldsAndInvalidTypedValues() {
    List<ApiFilter> unmappedField = List.of(new ApiFilter("deletedAt", FilterOperator.EQ, "null"));
    List<ApiFilter> invalidQuantity = List.of(new ApiFilter("quantity", FilterOperator.GT, "many"));

    assertThrows(FilterException.class, () -> find(unmappedField));
    assertThrows(FilterException.class, () -> find(invalidQuantity));
  }

  @Test
  void convertsIsoDatesAndEnumValuesAndRejectsInvalidValues() {
    List<ListingTestData.ListingAsset> dateResults =
        find(List.of(new ApiFilter("acquiredOn", FilterOperator.EQ, "2023-01-10")));
    List<ListingTestData.ListingAsset> enumResults =
        find(List.of(new ApiFilter("status", FilterOperator.EQ, "ACTIVE")));

    assertEquals(List.of("AAPL"), symbols(dateResults));
    assertEquals(List.of("AAPL", "MSFT"), symbols(enumResults).stream().sorted().toList());
    List<ApiFilter> invalidDate =
        List.of(new ApiFilter("acquiredOn", FilterOperator.EQ, "10/01/2023"));
    List<ApiFilter> invalidStatus = List.of(new ApiFilter("status", FilterOperator.EQ, "PENDING"));
    assertThrows(FilterException.class, () -> find(invalidDate));
    assertThrows(FilterException.class, () -> find(invalidStatus));
  }

  @Test
  void sqlNotEqualDoesNotIncludeRowsWithNullFieldValues() {
    List<ListingTestData.ListingAsset> result =
        find(List.of(new ApiFilter("status", FilterOperator.NEQ, "INACTIVE")));

    assertEquals(List.of("AAPL", "MSFT"), symbols(result).stream().sorted().toList());
  }

  @Test
  void matchesNullOnlyWhenTheFieldMappingConvertsTheInputToNull() {
    List<ListingTestData.ListingAsset> equalToNull =
        find(List.of(new ApiFilter("status", FilterOperator.EQ, "null")));
    List<ListingTestData.ListingAsset> notEqualToNull =
        find(List.of(new ApiFilter("status", FilterOperator.NEQ, "null")));

    assertEquals(List.of("BABA"), symbols(equalToNull));
    assertEquals(
        List.of("AAPL", "MSFT", "XOM"), symbols(notEqualToNull).stream().sorted().toList());
  }

  private List<ListingTestData.ListingAsset> find(List<ApiFilter> filters) {
    try (EntityManager entityManager = sessionFactory.createEntityManager()) {
      CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
      CriteriaQuery<ListingTestData.ListingAsset> query =
          criteriaBuilder.createQuery(ListingTestData.ListingAsset.class);
      Root<ListingTestData.ListingAsset> root = query.from(ListingTestData.ListingAsset.class);
      Specification<ListingTestData.ListingAsset> specification =
          builder.fromFilters(filters, mapping);
      query.select(root).where(specification.toPredicate(root, query, criteriaBuilder));
      return entityManager.createQuery(query).getResultList();
    }
  }

  private List<String> symbols(List<ListingTestData.ListingAsset> assets) {
    return assets.stream().map(ListingTestData.ListingAsset::getSymbol).toList();
  }
}

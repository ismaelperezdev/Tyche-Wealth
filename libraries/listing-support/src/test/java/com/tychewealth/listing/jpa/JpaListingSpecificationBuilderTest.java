package com.tychewealth.listing.jpa;

import static com.tychewealth.listing.ListingTestConstants.ACQUIRED_ON_FIELD;
import static com.tychewealth.listing.ListingTestConstants.ASSET_AAPL;
import static com.tychewealth.listing.ListingTestConstants.ASSET_BABA;
import static com.tychewealth.listing.ListingTestConstants.ASSET_MSFT;
import static com.tychewealth.listing.ListingTestConstants.ASSET_XOM;
import static com.tychewealth.listing.ListingTestConstants.DATE_AAPL_ACQUIRED;
import static com.tychewealth.listing.ListingTestConstants.NAME_APPLE;
import static com.tychewealth.listing.ListingTestConstants.NAME_EXXON;
import static com.tychewealth.listing.ListingTestConstants.NAME_FIELD;
import static com.tychewealth.listing.ListingTestConstants.NULL_TOKEN;
import static com.tychewealth.listing.ListingTestConstants.QUANTITY_FIELD;
import static com.tychewealth.listing.ListingTestConstants.STATUS_ACTIVE;
import static com.tychewealth.listing.ListingTestConstants.STATUS_FIELD;
import static com.tychewealth.listing.ListingTestConstants.STATUS_INACTIVE;
import static com.tychewealth.listing.ListingTestConstants.SYMBOL_FIELD;
import static com.tychewealth.listing.ListingTestConstants.TICKER_FIELD;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
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
                new ApiFilter(TICKER_FIELD, FilterOperator.EQ, ASSET_AAPL),
                new ApiFilter(QUANTITY_FIELD, FilterOperator.GTE, "2")));

    assertEquals(
        List.of(ASSET_AAPL), result.stream().map(ListingTestData.ListingAsset::getSymbol).toList());
  }

  @Test
  void treatsMultipleEqualityValuesAsAlternativesAndSupportsIn() {
    List<ListingTestData.ListingAsset> equalityResults =
        find(List.of(new ApiFilter(SYMBOL_FIELD, FilterOperator.EQ, ASSET_AAPL, ASSET_MSFT)));
    List<ListingTestData.ListingAsset> inResults =
        find(List.of(new ApiFilter(SYMBOL_FIELD, FilterOperator.IN, ASSET_AAPL, ASSET_MSFT)));

    assertEquals(2, equalityResults.size());
    assertEquals(2, inResults.size());
  }

  @Test
  void excludesEveryValueForNotEqualAndMatchesAnyValueForLike() {
    List<ListingTestData.ListingAsset> notEqualResults =
        find(List.of(new ApiFilter(SYMBOL_FIELD, FilterOperator.NEQ, ASSET_AAPL, ASSET_MSFT)));
    List<ListingTestData.ListingAsset> likeResults =
        find(List.of(new ApiFilter(NAME_FIELD, FilterOperator.LIKE, NAME_APPLE, NAME_EXXON)));

    assertEquals(
        List.of(ASSET_BABA, ASSET_XOM), symbols(notEqualResults).stream().sorted().toList());
    assertEquals(List.of(ASSET_AAPL, ASSET_XOM), symbols(likeResults).stream().sorted().toList());
  }

  @Test
  void returnsAllRowsForNoFiltersAndAppliesInclusiveAndExclusiveBoundaries() {
    List<ListingTestData.ListingAsset> noFilters = find(List.of());
    List<ListingTestData.ListingAsset> greaterThan =
        find(List.of(new ApiFilter(QUANTITY_FIELD, FilterOperator.GT, "3")));
    List<ListingTestData.ListingAsset> greaterThanOrEqual =
        find(List.of(new ApiFilter(QUANTITY_FIELD, FilterOperator.GTE, "3")));

    assertEquals(4, noFilters.size());
    assertEquals(List.of(ASSET_MSFT), symbols(greaterThan));
    assertEquals(
        List.of(ASSET_AAPL, ASSET_MSFT), symbols(greaterThanOrEqual).stream().sorted().toList());
  }

  @Test
  void supportsCaseInsensitiveSubstringFilters() {
    List<ListingTestData.ListingAsset> result =
        find(List.of(new ApiFilter(NAME_FIELD, FilterOperator.LIKE, "APP")));

    assertEquals(
        List.of(ASSET_AAPL), result.stream().map(ListingTestData.ListingAsset::getSymbol).toList());
  }

  @Test
  void treatsLikeWildcardsAndBackslashesAsLiteralSubstrings() {
    try (var session = sessionFactory.openSession()) {
      var transaction = session.beginTransaction();
      for (String name : List.of("Rate%USD", "Rate_USD", "Rate\\USD", "RateXUSD", "RateUSD")) {
        session.persist(new ListingTestData.ListingAsset(name, name, "Other", 1, null, null));
      }
      transaction.commit();
    }

    assertEquals(
        List.of("Rate%USD"),
        symbols(find(List.of(new ApiFilter(NAME_FIELD, FilterOperator.LIKE, "%")))));
    assertEquals(
        List.of("Rate_USD"),
        symbols(find(List.of(new ApiFilter(NAME_FIELD, FilterOperator.LIKE, "_")))));
    assertEquals(
        List.of("Rate\\USD"),
        symbols(find(List.of(new ApiFilter(NAME_FIELD, FilterOperator.LIKE, "\\")))));
    assertEquals(
        List.of("Rate%USD", "Rate_USD"),
        symbols(find(List.of(new ApiFilter(NAME_FIELD, FilterOperator.LIKE, "rate%u", "rate_u"))))
            .stream()
            .sorted()
            .toList());
  }

  @ParameterizedTest
  @ValueSource(strings = {"en-US", "de-DE", "fr-FR"})
  void acceptsOnlyIsoDatesRegardlessOfLocale(String languageTag) {
    LocaleContext previousLocaleContext = LocaleContextHolder.getLocaleContext();
    Locale locale = Locale.forLanguageTag(languageTag);
    try {
      LocaleContextHolder.setLocale(locale);
      assertEquals(
          List.of(ASSET_AAPL),
          symbols(
              find(
                  List.of(
                      new ApiFilter(ACQUIRED_ON_FIELD, FilterOperator.EQ, DATE_AAPL_ACQUIRED)))));

      String localizedDate =
          LocalDate.of(2023, 1, 10)
              .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale));
      List<ApiFilter> localizedDateFilter =
          List.of(new ApiFilter(ACQUIRED_ON_FIELD, FilterOperator.EQ, localizedDate));
      assertThrows(FilterException.class, () -> find(localizedDateFilter));
    } finally {
      LocaleContextHolder.setLocaleContext(previousLocaleContext);
    }
  }

  @Test
  void rejectsUnmappedFieldsAndInvalidTypedValues() {
    List<ApiFilter> unmappedField =
        List.of(new ApiFilter("deletedAt", FilterOperator.EQ, NULL_TOKEN));
    List<ApiFilter> invalidQuantity =
        List.of(new ApiFilter(QUANTITY_FIELD, FilterOperator.GT, "many"));

    assertThrows(FilterException.class, () -> find(unmappedField));
    assertThrows(FilterException.class, () -> find(invalidQuantity));
  }

  @Test
  void convertsIsoDatesAndEnumValuesAndRejectsInvalidValues() {
    List<ListingTestData.ListingAsset> dateResults =
        find(List.of(new ApiFilter(ACQUIRED_ON_FIELD, FilterOperator.EQ, DATE_AAPL_ACQUIRED)));
    List<ListingTestData.ListingAsset> enumResults =
        find(List.of(new ApiFilter(STATUS_FIELD, FilterOperator.EQ, STATUS_ACTIVE)));

    assertEquals(List.of(ASSET_AAPL), symbols(dateResults));
    assertEquals(List.of(ASSET_AAPL, ASSET_MSFT), symbols(enumResults).stream().sorted().toList());
    List<ApiFilter> invalidDate =
        List.of(new ApiFilter(ACQUIRED_ON_FIELD, FilterOperator.EQ, "10/01/2023"));
    List<ApiFilter> invalidStatus =
        List.of(new ApiFilter(STATUS_FIELD, FilterOperator.EQ, "PENDING"));
    assertThrows(FilterException.class, () -> find(invalidDate));
    assertThrows(FilterException.class, () -> find(invalidStatus));
  }

  @Test
  void sqlNotEqualDoesNotIncludeRowsWithNullFieldValues() {
    List<ListingTestData.ListingAsset> result =
        find(List.of(new ApiFilter(STATUS_FIELD, FilterOperator.NEQ, STATUS_INACTIVE)));

    assertEquals(List.of(ASSET_AAPL, ASSET_MSFT), symbols(result).stream().sorted().toList());
  }

  @Test
  void matchesNullOnlyWhenTheFieldMappingConvertsTheInputToNull() {
    List<ListingTestData.ListingAsset> equalToNull =
        find(List.of(new ApiFilter(STATUS_FIELD, FilterOperator.EQ, NULL_TOKEN)));
    List<ListingTestData.ListingAsset> notEqualToNull =
        find(List.of(new ApiFilter(STATUS_FIELD, FilterOperator.NEQ, NULL_TOKEN)));

    assertEquals(List.of(ASSET_BABA), symbols(equalToNull));
    assertEquals(
        List.of(ASSET_AAPL, ASSET_MSFT, ASSET_XOM),
        symbols(notEqualToNull).stream().sorted().toList());
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

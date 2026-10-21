package com.tychewealth.listing.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tychewealth.listing.domain.ApiFilter;
import com.tychewealth.listing.dto.ExcludeFilter;
import com.tychewealth.listing.dto.Filterable;
import com.tychewealth.listing.enums.ErrorDefinition;
import com.tychewealth.listing.enums.FilterOperator;
import com.tychewealth.listing.exception.FilterException;
import com.tychewealth.listing.properties.FilterProperties;
import java.text.MessageFormat;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;

class FilterParserTest {

  private final FilterParser parser = new FilterParser(new FilterProperties());

  @Test
  void parsesPaginationSortAndFieldFilter() {
    ListQuery result =
        parser.parse(
            Map.of("offset", "12", "limit", "15", "sort", "-name", "name", "Ada"), FilterDto.class);

    assertEquals(12, result.pageCriteria().offset());
    assertEquals(15, result.pageCriteria().limit());
    assertEquals("name", result.pageCriteria().sorts().getFirst().field());
    assertEquals("Ada", result.filter().getFirst().singleValue());
    assertEquals(FilterOperator.EQ, result.filter().getFirst().operator());
    assertNull(result.range());
  }

  @Test
  void extractsValueFromOperatorInParameterName() {
    ListQuery result = parser.parse(Map.of("age>=18", ""), FilterDto.class);

    ApiFilter filter = result.filter().getFirst();
    assertEquals("age", filter.field());
    assertEquals(FilterOperator.GTE, filter.operator());
    assertEquals("18", filter.singleValue());
  }

  @Test
  void fieldOperatorsOverrideClassLevelDefaults() {
    FilterParser classLevelParser = new FilterParser(new FilterProperties());
    Map<String, String> params = Map.of("age.gt", "18");

    FilterException exception =
        assertThrows(
            FilterException.class, () -> classLevelParser.parse(params, RestrictedFilterDto.class));

    assertEquals(ErrorDefinition.FILTER_OPERATOR_NOT_ALLOWED, exception.getErrorDefinition());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }

  @Test
  void appliesDefaultAndMaximumPaginationValues() {
    FilterProperties properties = new FilterProperties();
    properties.setMaxOffset(50);
    properties.setMaxLimit(100);
    FilterParser configuredParser = new FilterParser(properties);

    ListQuery result =
        configuredParser.parse(Map.of("offset", "60", "limit", "200"), FilterDto.class);

    assertEquals(50, result.pageCriteria().offset());
    assertEquals(100, result.pageCriteria().limit());
    assertEquals(0, configuredParser.parse(Map.of(), FilterDto.class).pageCriteria().offset());
    assertEquals(20, configuredParser.parse(Map.of(), FilterDto.class).pageCriteria().limit());
  }

  @Test
  void parsesRangeHeaderWhenPaginationParametersAreAbsent() {
    ListQuery result = parser.parse(Map.of(), "items=20-29", FilterDto.class);

    assertEquals(20, result.pageCriteria().offset());
    assertEquals(10, result.pageCriteria().limit());
    assertEquals("items 20-29/0", result.range().toContentRangeHeader());
  }

  @ParameterizedTest
  @CsvSource({
    "records=60-69, 50, 10, records 50-59/0",
    "records=20-219, 20, 100, records 20-119/0",
    "records=60-259, 50, 100, records 50-149/0"
  })
  void keepsRangeAlignedWithCappedPagination(
      String header, int expectedOffset, int expectedLimit, String expectedContentRange) {
    FilterProperties properties = new FilterProperties();
    properties.setMaxOffset(50);
    properties.setMaxLimit(100);
    FilterParser configuredParser = new FilterParser(properties);

    ListQuery result = configuredParser.parse(Map.of(), header, FilterDto.class);

    assertEquals(expectedOffset, result.pageCriteria().offset());
    assertEquals(expectedLimit, result.pageCriteria().limit());
    assertEquals(expectedOffset, result.range().offset());
    assertEquals(expectedLimit, result.range().limit());
    assertEquals("records", result.range().name());
    assertEquals(expectedContentRange, result.range().toContentRangeHeader());
  }

  @Test
  void rejectsUnknownFilterAndSortFields() {
    Map<String, String> unknownFilter = Map.of("missing", "value");
    Map<String, String> unknownSort = Map.of("sort", "-missing");

    assertThrows(FilterException.class, () -> parser.parse(unknownFilter, FilterDto.class));
    assertThrows(FilterException.class, () -> parser.parse(unknownSort, FilterDto.class));
  }

  @Test
  void rejectsTooManyFiltersWithFilterSpecificError() {
    FilterProperties properties = new FilterProperties();
    properties.setMaxFilters(1);
    FilterParser configuredParser = new FilterParser(properties);
    Map<String, String> params = Map.of("name", "Ada", "age", "18");

    FilterException exception =
        assertThrows(FilterException.class, () -> configuredParser.parse(params, FilterDto.class));

    assertEquals(ErrorDefinition.FILTER_MAX_EXCEEDED, exception.getErrorDefinition());
    assertEquals(
        ErrorDefinition.FILTER_MAX_EXCEEDED.getCode(), exception.getErrorDefinition().getCode());
    assertEquals(
        ErrorDefinition.FILTER_MAX_EXCEEDED.getType(), exception.getErrorDefinition().getType());
    assertEquals(
        MessageFormat.format(ErrorDefinition.FILTER_MAX_EXCEEDED.getDescription(), 1, 2),
        exception.getMessage());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }

  @Test
  void rejectsTooManySortsWithSortSpecificError() {
    FilterProperties properties = new FilterProperties();
    properties.setMaxSorts(1);
    FilterParser configuredParser = new FilterParser(properties);
    Map<String, String> params = Map.of("sort", "name,-age");

    FilterException exception =
        assertThrows(FilterException.class, () -> configuredParser.parse(params, FilterDto.class));

    assertEquals(ErrorDefinition.SORT_MAX_EXCEEDED, exception.getErrorDefinition());
    assertEquals(
        ErrorDefinition.SORT_MAX_EXCEEDED.getCode(), exception.getErrorDefinition().getCode());
    assertEquals(
        ErrorDefinition.SORT_MAX_EXCEEDED.getType(), exception.getErrorDefinition().getType());
    assertEquals(
        MessageFormat.format(ErrorDefinition.SORT_MAX_EXCEEDED.getDescription(), 1, 2),
        exception.getMessage());
    assertEquals(HttpStatus.BAD_REQUEST, exception.getHttpStatus());
  }

  @Test
  void excludesStaticSyntheticAndExplicitlyExcludedFields() {
    for (String field :
        new String[] {"constant", "nested.value", "hidden", "hiddenNested.value", "this$0"}) {
      Map<String, String> filterParams = Map.of(field, "x");
      Map<String, String> sortParams = Map.of("sort", field);
      assertThrows(
          FilterException.class, () -> parser.parse(filterParams, InstanceDto.class), field);
      assertThrows(FilterException.class, () -> parser.parse(sortParams, InstanceDto.class), field);
    }
    assertEquals(1, parser.parse(Map.of("value", "x"), InstanceDto.class).filter().size());
  }

  @Test
  void isolatesCachedMetadataBetweenParserConfigurations() {
    FilterProperties shallowProperties = new FilterProperties();
    shallowProperties.setMaxNestedDepth(0);
    FilterParser shallowParser = new FilterParser(shallowProperties);
    FilterParser deepParser = new FilterParser(new FilterProperties());
    shallowParser.parse(Map.of(), BranchDto.class);
    assertEquals(1, deepParser.parse(Map.of("leaf.value", "x"), BranchDto.class).filter().size());
    Map<String, String> params = Map.of("leaf.value", "x");
    assertThrows(FilterException.class, () -> shallowParser.parse(params, BranchDto.class));
  }

  @Test
  void computesNestedMetadataAtTheActualDepthRegardlessOfCacheOrder() {
    for (boolean nestedFirst : new boolean[] {true, false}) {
      FilterProperties properties = new FilterProperties();
      properties.setMaxNestedDepth(1);
      FilterParser configuredParser = new FilterParser(properties);
      if (nestedFirst) {
        configuredParser.parse(Map.of(), RootDto.class);
      }
      assertEquals(
          1, configuredParser.parse(Map.of("leaf.value", "x"), BranchDto.class).filter().size());
      Map<String, String> tooDeepParams = Map.of("branch.leaf.value", "x");
      assertThrows(
          FilterException.class, () -> configuredParser.parse(tooDeepParams, RootDto.class));
    }
  }

  @Test
  void boundsRecursiveMetadataByConfiguredDepth() {
    FilterProperties properties = new FilterProperties();
    properties.setMaxNestedDepth(1);
    FilterParser configuredParser = new FilterParser(properties);
    assertEquals(
        1, configuredParser.parse(Map.of("child.value", "x"), RecursiveDto.class).filter().size());
    Map<String, String> tooDeepParams = Map.of("child.child.value", "x");
    assertThrows(
        FilterException.class, () -> configuredParser.parse(tooDeepParams, RecursiveDto.class));
  }

  @Filterable
  private class InstanceDto {
    @Filterable static String constant;
    static LeafDto nested;
    String value;
    @ExcludeFilter String hidden;
    @ExcludeFilter LeafDto hiddenNested;
  }

  @Filterable
  private static class RootDto {
    BranchDto branch;
  }

  @Filterable
  private static class BranchDto {
    LeafDto leaf;
  }

  @Filterable
  private static class LeafDto {
    String value;
  }

  @Filterable
  private static class RecursiveDto {
    String value;
    RecursiveDto child;
  }

  @Filterable
  private static class FilterDto {
    String name;
    int age;
  }

  @Filterable
  private static class RestrictedFilterDto {
    @Filterable(operators = FilterOperator.EQ)
    int age;
  }
}

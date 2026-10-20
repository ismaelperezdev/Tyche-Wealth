package com.tychewealth.listing.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tychewealth.listing.domain.ApiFilter;
import com.tychewealth.listing.dto.Filterable;
import com.tychewealth.listing.enums.ErrorDefinition;
import com.tychewealth.listing.enums.FilterOperator;
import com.tychewealth.listing.exception.FilterException;
import com.tychewealth.listing.properties.FilterProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;
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

    FilterException exception =
        assertThrows(
            FilterException.class,
            () -> classLevelParser.parse(Map.of("age.gt", "18"), RestrictedFilterDto.class));

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

  @Test
  void rejectsUnknownFilterAndSortFields() {
    Map<String, String> unknownFilter = Map.of("missing", "value");
    Map<String, String> unknownSort = Map.of("sort", "-missing");

    assertThrows(FilterException.class, () -> parser.parse(unknownFilter, FilterDto.class));
    assertThrows(FilterException.class, () -> parser.parse(unknownSort, FilterDto.class));
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

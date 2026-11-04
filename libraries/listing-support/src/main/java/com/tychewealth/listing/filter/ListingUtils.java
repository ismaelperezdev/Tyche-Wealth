package com.tychewealth.listing.filter;

import com.tychewealth.listing.domain.ApiFilter;
import com.tychewealth.listing.domain.PageCriteria;
import com.tychewealth.listing.domain.SortCriteria;
import com.tychewealth.listing.enums.FilterOperator;
import com.tychewealth.listing.shared.FilterFieldMapping;
import com.tychewealth.listing.shared.OffsetBasedPageRequest;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Utility methods for working with parsed API filters. */
public final class ListingUtils {

  private ListingUtils() {}

  public static Optional<ApiFilter> findFilter(
      List<ApiFilter> filters, String field, FilterOperator operator) {
    return filters.stream()
        .filter(filter -> filter.field().equals(field))
        .filter(filter -> operator == null || filter.operator() == operator)
        .findFirst();
  }

  public static List<ApiFilter> findFilters(
      List<ApiFilter> filters, String field, Set<FilterOperator> operators) {
    return filters.stream()
        .filter(filter -> filter.field().equals(field))
        .filter(filter -> operators.contains(filter.operator()))
        .toList();
  }

  public static OffsetBasedPageRequest pageRequest(
      PageCriteria criteria, FilterFieldMapping fieldMapping, SortCriteria defaultSort) {
    return pageRequest(criteria, fieldMapping, defaultSort, null);
  }

  public static OffsetBasedPageRequest pageRequest(
      PageCriteria criteria,
      FilterFieldMapping fieldMapping,
      SortCriteria defaultSort,
      SortCriteria tieBreaker) {
    List<SortCriteria> sorts =
        criteria.sorts() == null || criteria.sorts().isEmpty()
            ? new java.util.ArrayList<>(List.of(defaultSort))
            : new java.util.ArrayList<>(criteria.sorts());
    if (tieBreaker != null
        && sorts.stream().noneMatch(sort -> sort.field().equals(tieBreaker.field()))) {
      sorts.add(tieBreaker);
    }
    PageCriteria effectiveCriteria = new PageCriteria(criteria.offset(), criteria.limit(), sorts);
    return new OffsetBasedPageRequest(effectiveCriteria, fieldMapping);
  }
}

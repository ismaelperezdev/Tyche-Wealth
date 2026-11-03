package com.tychewealth.listing.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.tychewealth.listing.domain.ApiFilter;
import com.tychewealth.listing.domain.PageCriteria;
import com.tychewealth.listing.domain.SortCriteria;
import com.tychewealth.listing.enums.FilterOperator;
import com.tychewealth.listing.enums.SortDirection;
import com.tychewealth.listing.shared.FilterFieldMapping;
import com.tychewealth.listing.shared.OffsetBasedPageRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ListingUtilsTest {

  @Test
  void findsFilterByFieldAndOperator() {
    ApiFilter matchingFilter =
        new ApiFilter("capturedAt", FilterOperator.GTE, "2026-08-27T12:00:00Z");
    List<ApiFilter> filters =
        List.of(
            new ApiFilter("symbol", FilterOperator.IN, "AAPL"),
            matchingFilter,
            new ApiFilter("capturedAt", FilterOperator.LTE, "2026-08-27T18:00:00Z"));

    assertThat(ListingUtils.findFilter(filters, "capturedAt", FilterOperator.GTE))
        .contains(matchingFilter);
  }

  @Test
  void findsFirstFilterForFieldWhenOperatorIsUnspecified() {
    ApiFilter firstFilter = new ApiFilter("capturedAt", FilterOperator.GTE, "from");
    List<ApiFilter> filters =
        List.of(firstFilter, new ApiFilter("capturedAt", FilterOperator.LTE, "to"));

    assertThat(ListingUtils.findFilter(filters, "capturedAt", null)).contains(firstFilter);
  }

  @Test
  void returnsEmptyWhenNoFilterMatches() {
    assertThat(ListingUtils.findFilter(List.of(), "symbol", FilterOperator.EQ)).isEmpty();
  }

  @Test
  void appliesDefaultSortOnlyWhenNoSortWasRequested() {
    FilterFieldMapping mapping =
        new FilterFieldMapping() {
          @Override
          protected Map<String, String> fieldNameMap() {
            return Map.of("id", "id", "createdAt", "createdAt");
          }
        };
    SortCriteria defaultSort = new SortCriteria("id", SortDirection.ASC);

    OffsetBasedPageRequest withDefault =
        ListingUtils.pageRequest(new PageCriteria(0, 10, List.of()), mapping, defaultSort);
    OffsetBasedPageRequest withRequestedSort =
        ListingUtils.pageRequest(
            new PageCriteria(5, 10, List.of(new SortCriteria("createdAt", SortDirection.DESC))),
            mapping,
            defaultSort);

    assertThat(withDefault.getSort().getOrderFor("id")).isNotNull();
    assertThat(withDefault.getSort().getOrderFor("id").getDirection())
        .isEqualTo(org.springframework.data.domain.Sort.Direction.ASC);
    assertThat(withRequestedSort.getOffset()).isEqualTo(5);
    assertThat(withRequestedSort.getSort().getOrderFor("createdAt").getDirection())
        .isEqualTo(org.springframework.data.domain.Sort.Direction.DESC);
  }

  @Test
  void appendsConfiguredTieBreakerAfterRequestedSortWithoutDuplicates() {
    FilterFieldMapping mapping =
        new FilterFieldMapping() {
          @Override
          protected Map<String, String> fieldNameMap() {
            return Map.of("id", "id", "createdAt", "createdAt");
          }
        };

    OffsetBasedPageRequest pageRequest =
        ListingUtils.pageRequest(
            new PageCriteria(
                0,
                10,
                List.of(
                    new SortCriteria("createdAt", SortDirection.DESC),
                    new SortCriteria("id", SortDirection.DESC))),
            mapping,
            new SortCriteria("id", SortDirection.ASC),
            new SortCriteria("id", SortDirection.ASC));

    assertThat(
            pageRequest.getSort().stream()
                .map(org.springframework.data.domain.Sort.Order::getProperty))
        .containsExactly("createdAt", "id");
    assertThat(pageRequest.getSort().getOrderFor("id").getDirection())
        .isEqualTo(org.springframework.data.domain.Sort.Direction.DESC);
  }

  @Test
  void appendsConfiguredTieBreakerToRequestedSort() {
    FilterFieldMapping mapping =
        new FilterFieldMapping() {
          @Override
          protected Map<String, String> fieldNameMap() {
            return Map.of("id", "id", "createdAt", "createdAt");
          }
        };

    OffsetBasedPageRequest pageRequest =
        ListingUtils.pageRequest(
            new PageCriteria(0, 10, List.of(new SortCriteria("createdAt", SortDirection.DESC))),
            mapping,
            new SortCriteria("id", SortDirection.ASC),
            new SortCriteria("id", SortDirection.ASC));

    assertThat(
            pageRequest.getSort().stream()
                .map(org.springframework.data.domain.Sort.Order::getProperty))
        .containsExactly("createdAt", "id");
  }
}

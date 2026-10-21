package com.tychewealth.listing.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tychewealth.listing.domain.PageCriteria;
import com.tychewealth.listing.domain.SortCriteria;
import com.tychewealth.listing.enums.SortDirection;
import com.tychewealth.listing.exception.FilterException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class OffsetBasedPageRequestTest {

  @Test
  void exposesOffsetLimitAndSortFromCriteria() {
    OffsetBasedPageRequest request =
        new OffsetBasedPageRequest(
            new PageCriteria(25, 10, List.of(new SortCriteria("createdAt", SortDirection.DESC))));

    assertEquals(2, request.getPageNumber());
    assertEquals(10, request.getPageSize());
    assertEquals(25, request.getOffset());
    assertEquals(Sort.Direction.DESC, request.getSort().getOrderFor("createdAt").getDirection());
  }

  @Test
  void navigatesUsingOffsetRatherThanPageNumber() {
    OffsetBasedPageRequest request = new OffsetBasedPageRequest(15, 10);

    assertTrue(request.hasPrevious());
    assertEquals(25, request.next().getOffset());
    assertEquals(5, request.previousOrFirst().getOffset());
    assertEquals(0, request.first().getOffset());
    assertEquals(30, request.withPage(3).getOffset());
  }

  @Test
  void rejectsInvalidPaginationValues() {
    assertThrows(FilterException.class, () -> new OffsetBasedPageRequest(-1, 10));
    assertThrows(FilterException.class, () -> new OffsetBasedPageRequest(0, 0));
    OffsetBasedPageRequest request = new OffsetBasedPageRequest(0, 10);
    assertThrows(IllegalArgumentException.class, () -> request.withPage(-1));
  }

  @Test
  void firstPageHasNoPreviousPage() {
    OffsetBasedPageRequest request = new OffsetBasedPageRequest(0, 10);

    assertFalse(request.hasPrevious());
    assertEquals(0, request.previousOrFirst().getOffset());
  }

  @Test
  void mapsApiSortFieldsToJpaProperties() {
    FilterFieldMapping mapping =
        new FilterFieldMapping() {
          @Override
          protected Map<String, String> fieldNameMap() {
            return Map.of("displayName", "name");
          }
        };
    OffsetBasedPageRequest request =
        new OffsetBasedPageRequest(
            new PageCriteria(0, 10, List.of(new SortCriteria("displayName", SortDirection.ASC))),
            mapping);

    assertTrue(request.getSort().getOrderFor("name").isAscending());
    PageCriteria invalidCriteria =
        new PageCriteria(0, 10, List.of(new SortCriteria("unknown", SortDirection.ASC)));
    assertThrows(FilterException.class, () -> new OffsetBasedPageRequest(invalidCriteria, mapping));
  }
}

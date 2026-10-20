package com.tychewealth.listing.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tychewealth.listing.domain.PageCriteria;
import com.tychewealth.listing.exception.FilterException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ListQueryArgumentResolverTest {

  private final ListQueryArgumentResolverTestHelper helper =
      new ListQueryArgumentResolverTestHelper();

  @Test
  void supportsListQueryParametersOnlyOnListableEndpoints() {
    assertTrue(helper.supportsListableEndpoint());
    assertFalse(helper.supportsNonListableEndpoint());
  }

  @Test
  void parsesOffsetFiltersAndSortForTheEndpointResponseDto() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("offset", "7");
    request.addParameter("limit", "5");
    request.addParameter("symbol", "AAPL");
    request.addParameter("sort", "-name");

    ListQuery query = helper.resolve(request);

    assertEquals(7, query.pageCriteria().offset());
    assertEquals(5, query.pageCriteria().limit());
    assertEquals("AAPL", query.filter().getFirst().singleValue());
    assertEquals("name", query.pageCriteria().sorts().getFirst().field());
  }

  @Test
  void convertsLegacyPageToOffsetAndClampsNegativePagesToTheFirstPage() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("page", "3");
    request.addParameter("limit", "10");

    assertEquals(30, helper.resolve(request).pageCriteria().offset());

    request.setParameter("page", "-2");
    assertEquals(0, helper.resolve(request).pageCriteria().offset());
  }

  @Test
  void rejectsNonNumericLegacyPageValues() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addParameter("page", "many");

    assertThrows(FilterException.class, () -> helper.resolve(request));
  }

  @Test
  void appliesRangeHeaderForOffsetBasedRequests() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Range", "assets=10-19");

    PageCriteria criteria = helper.resolve(request).pageCriteria();

    assertEquals(10, criteria.offset());
    assertEquals(10, criteria.limit());
  }
}

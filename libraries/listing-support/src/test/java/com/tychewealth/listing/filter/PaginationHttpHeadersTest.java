package com.tychewealth.listing.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tychewealth.listing.enums.ErrorDefinition;
import com.tychewealth.listing.exception.FilterException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class PaginationHttpHeadersTest {

  @Test
  void buildsNavigationLinksPreservingOtherQueryParameters() {
    HttpHeaders headers =
        PaginationHttpHeaders.build(
            "/api/items", 5, 5, 12, "status=OPEN&offset=0&limit=99&sort=-createdAt");

    assertEquals("12", headers.getFirst("X-Total-Count"));
    String links = headers.getFirst(HttpHeaders.LINK);
    assertTrue(
        links.contains("</api/items?status=OPEN&sort=-createdAt&offset=5&limit=5>; rel=\"self\""));
    assertTrue(links.contains("offset=0&limit=5>; rel=\"first\""));
    assertTrue(links.contains("offset=0&limit=5>; rel=\"prev\""));
    assertTrue(links.contains("offset=10&limit=5>; rel=\"next\""));
    assertTrue(links.contains("offset=10&limit=5>; rel=\"last\""));
  }

  @Test
  void emitsOnlySelfLinkForAnEmptyFirstPage() {
    HttpHeaders headers = PaginationHttpHeaders.build("/api/items", 0, 10, 0, null);

    assertEquals("0", headers.getFirst("X-Total-Count"));
    assertEquals(
        "</api/items?offset=0&limit=10>; rel=\"self\"", headers.getFirst(HttpHeaders.LINK));
  }

  @Test
  void omitsUnrepresentableNextAndLastOffsetsInsteadOfOverflowing() {
    HttpHeaders headers =
        PaginationHttpHeaders.build(
            "/api/items", Integer.MAX_VALUE, 1, (long) Integer.MAX_VALUE + 5, "");

    String links = headers.getFirst(HttpHeaders.LINK);
    assertFalse(links.contains("rel=\"next\""));
    assertFalse(links.contains("rel=\"last\""));
    assertFalse(links.contains("offset=-"));
  }

  @Test
  void stripsControlCharactersFromHeaderInputs() {
    HttpHeaders headers =
        PaginationHttpHeaders.build("/api/items\r\nX-Evil: yes", 0, 10, 0, "q=one\r\ntwo");

    String links = headers.getFirst(HttpHeaders.LINK);
    assertFalse(links.contains("\r"));
    assertFalse(links.contains("\n"));
  }

  @Test
  void rejectsInvalidPaginationArguments() {
    FilterException offsetException =
        assertThrows(
            FilterException.class, () -> PaginationHttpHeaders.build("/api/items", -1, 10, 0, ""));
    FilterException limitException =
        assertThrows(
            FilterException.class, () -> PaginationHttpHeaders.build("/api/items", 0, 0, 0, ""));
    FilterException totalException =
        assertThrows(
            FilterException.class, () -> PaginationHttpHeaders.build("/api/items", 0, 10, -1, ""));

    assertEquals(ErrorDefinition.PAGINATION_INVALID_OFFSET, offsetException.getErrorDefinition());
    assertEquals(ErrorDefinition.PAGINATION_INVALID_LIMIT, limitException.getErrorDefinition());
    assertEquals(ErrorDefinition.PAGINATION_INVALID_TOTAL, totalException.getErrorDefinition());
  }
}

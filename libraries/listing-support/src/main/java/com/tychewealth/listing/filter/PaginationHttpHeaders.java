package com.tychewealth.listing.filter;

import com.tychewealth.listing.enums.ErrorDefinition;
import com.tychewealth.listing.exception.FilterException;
import java.util.StringJoiner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/**
 * Builds pagination response headers for an offset-based result window.
 *
 * <p>The output includes {@code X-Total-Count} and {@code Link} relations for current, first,
 * previous, next, and last windows when applicable. Existing query parameters are retained while
 * offset and limit are replaced; unsafe header characters are removed before formatting.
 */
@Slf4j
public final class PaginationHttpHeaders {

  private PaginationHttpHeaders() {}

  private static final String HEADER_X_TOTAL_COUNT = "X-Total-Count";
  private static final String HEADER_LINK = "Link";
  private static final String LINK_TEMPLATE = "<%s?%s>; rel=\"%s\"";
  private static final String REL_SELF = "self";
  private static final String REL_FIRST = "first";
  private static final String REL_PREV = "prev";
  private static final String REL_NEXT = "next";
  private static final String REL_LAST = "last";

  public static HttpHeaders build(
      String requestUri, int offset, int limit, long totalElements, String queryString) {

    if (offset < 0) {
      throw new FilterException(ErrorDefinition.PAGINATION_INVALID_OFFSET, HttpStatus.BAD_REQUEST);
    }
    if (limit < 1) {
      throw new FilterException(ErrorDefinition.PAGINATION_INVALID_LIMIT, HttpStatus.BAD_REQUEST);
    }
    if (totalElements < 0) {
      throw new FilterException(ErrorDefinition.PAGINATION_INVALID_TOTAL, HttpStatus.BAD_REQUEST);
    }

    String resolvedUri = requestUri != null ? requestUri : "";
    String resolvedQuery = queryString != null ? queryString : "";

    String sanitisedUri = sanitise(resolvedUri);
    String sanitisedQuery = sanitise(resolvedQuery);

    HttpHeaders headers = new HttpHeaders();
    headers.add(HEADER_X_TOTAL_COUNT, String.valueOf(totalElements));

    String links = buildLinks(sanitisedUri, sanitisedQuery, offset, limit, totalElements);
    if (!links.isEmpty()) {
      headers.add(HEADER_LINK, links);
    }

    return headers;
  }

  private static String buildLinks(
      String uri, String baseQuery, int offset, int limit, long totalElements) {
    StringJoiner joiner = new StringJoiner(", ");

    String selfQuery = rebuildQueryString(baseQuery, offset, limit);
    joiner.add(formatLink(uri, selfQuery, REL_SELF));

    if (offset != 0) {
      String firstQuery = rebuildQueryString(baseQuery, 0, limit);
      joiner.add(formatLink(uri, firstQuery, REL_FIRST));
    }

    if (offset > 0) {
      int prevOffset = clampOffset(offset - limit);
      String prevQuery = rebuildQueryString(baseQuery, prevOffset, limit);
      joiner.add(formatLink(uri, prevQuery, REL_PREV));
    }

    long nextOffset = (long) offset + limit;
    if (nextOffset < totalElements && nextOffset <= Integer.MAX_VALUE) {
      String nextQuery = rebuildQueryString(baseQuery, (int) nextOffset, limit);
      joiner.add(formatLink(uri, nextQuery, REL_NEXT));
    }

    long lastOffset = lastPageOffset(limit, totalElements);

    if (lastOffset != offset && lastOffset <= Integer.MAX_VALUE) {
      String lastQuery = rebuildQueryString(baseQuery, (int) lastOffset, limit);
      joiner.add(formatLink(uri, lastQuery, REL_LAST));
    }

    return joiner.toString();
  }

  private static String sanitise(String input) {
    if (input == null) {
      return "";
    }
    String sanitised = input.replaceAll("[\\x00-\\x08\\x0A-\\x1F\"\\\\<>]", "");
    if (!input.equals(sanitised) && log.isWarnEnabled()) {
      log.warn(
          "Sanitised {} control character(s) from pagination header input",
          input.length() - sanitised.length());
    }
    return sanitised;
  }

  private static String rebuildQueryString(String baseQuery, int offset, int limit) {
    StringBuilder sb = new StringBuilder();
    if (baseQuery != null && !baseQuery.isBlank()) {
      String[] params = baseQuery.split("&");
      for (String param : params) {
        if (!param.startsWith("offset=") && !param.startsWith("limit=") && !param.isEmpty()) {
          if (!sb.isEmpty()) {
            sb.append('&');
          }
          sb.append(param);
        }
      }
    }
    if (!sb.isEmpty()) {
      sb.append('&');
    }
    sb.append("offset=").append(offset).append("&limit=").append(limit);
    return sb.toString();
  }

  private static String formatLink(String uri, String query, String rel) {
    return String.format(LINK_TEMPLATE, uri, query, rel);
  }

  private static int clampOffset(int offset) {
    return Math.max(0, offset);
  }

  private static long lastPageOffset(int limit, long totalElements) {
    if (totalElements == 0) {
      return 0L;
    }
    return ((totalElements - 1) / limit) * limit;
  }
}

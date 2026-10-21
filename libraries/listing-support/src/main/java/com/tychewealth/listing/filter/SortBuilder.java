package com.tychewealth.listing.filter;

import com.tychewealth.listing.domain.SortCriteria;
import com.tychewealth.listing.enums.ErrorDefinition;
import com.tychewealth.listing.enums.SortDirection;
import com.tychewealth.listing.exception.FilterException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;

/**
 * Parses comma-separated sort expressions into {@link SortCriteria} values.
 *
 * <p>Unprefixed and {@code +}-prefixed fields sort ascending; a {@code -} prefix sorts descending.
 * Blank tokens are ignored, and the overload with a maximum rejects requests with too many fields.
 */
public class SortBuilder {

  private SortBuilder() {}

  public static List<SortCriteria> parse(String sort) {
    if (sort == null || sort.isBlank()) {
      return Collections.emptyList();
    }

    return Arrays.stream(sort.split(","))
        .map(SortBuilder::toSortCriteria)
        .flatMap(Optional::stream)
        .toList();
  }

  public static List<SortCriteria> parse(String sort, int maxSorts) {
    List<SortCriteria> result = parse(sort);
    if (result.size() > maxSorts) {
      throw new FilterException(
          ErrorDefinition.SORT_MAX_EXCEEDED,
          new Object[] {maxSorts, result.size()},
          HttpStatus.BAD_REQUEST);
    }
    return result;
  }

  private static Optional<SortCriteria> toSortCriteria(String token) {
    String trimmed = token.trim();
    if (trimmed.isEmpty()) return Optional.empty();

    if (trimmed.length() > 1 && trimmed.startsWith("-")) {
      return Optional.of(new SortCriteria(trimmed.substring(1), SortDirection.DESC));
    }

    if (trimmed.length() > 1 && trimmed.startsWith("+")) {
      return Optional.of(new SortCriteria(trimmed.substring(1), SortDirection.ASC));
    }

    if (!trimmed.startsWith("-") && !trimmed.startsWith("+")) {
      return Optional.of(new SortCriteria(trimmed, SortDirection.ASC));
    }
    return Optional.empty();
  }
}

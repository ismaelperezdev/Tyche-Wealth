package com.tychewealth.listing.filter;

import com.tychewealth.listing.enums.ErrorDefinition;
import com.tychewealth.listing.exception.FilterException;
import org.springframework.http.HttpStatus;

/**
 * Parsed representation of an item range, such as {@code items=20-29}.
 *
 * <p>The inclusive endpoints are stored as an offset and a count. The total is zero until the
 * caller supplies a result count with {@link #withTotalElements(long)}.
 */
public record Range(String name, int offset, int limit, long totalElements) {

  public Range(String name, int offset, int limit) {
    this(name, offset, limit, 0L);
  }

  public Range withTotalElements(long totalElements) {
    return new Range(name, offset, limit, totalElements);
  }

  public String toContentRangeHeader() {
    int finish = offset + limit - 1;
    return name + " " + offset + "-" + finish + "/" + totalElements;
  }

  public static Range parse(String rangeHeader) {
    if (rangeHeader == null || rangeHeader.isBlank()) {
      throw new FilterException(
          ErrorDefinition.RANGE_PARSE_ERROR, new Object[] {rangeHeader}, HttpStatus.BAD_REQUEST);
    }
    int eqIdx = rangeHeader.indexOf('=');
    if (eqIdx < 0) {
      throw new FilterException(
          ErrorDefinition.RANGE_PARSE_ERROR, new Object[] {rangeHeader}, HttpStatus.BAD_REQUEST);
    }
    String name = rangeHeader.substring(0, eqIdx).trim();
    int dashIdx = rangeHeader.indexOf('-', eqIdx + 1);
    if (dashIdx < 0) {
      throw new FilterException(
          ErrorDefinition.RANGE_PARSE_ERROR, new Object[] {rangeHeader}, HttpStatus.BAD_REQUEST);
    }
    int init;
    int finish;
    try {
      init = Integer.parseInt(rangeHeader.substring(eqIdx + 1, dashIdx).trim());
      finish = Integer.parseInt(rangeHeader.substring(dashIdx + 1).trim());
    } catch (NumberFormatException e) {
      throw new FilterException(
          ErrorDefinition.RANGE_INVALID_VALUES, new Object[] {rangeHeader}, HttpStatus.BAD_REQUEST);
    }
    if (init < 0 || finish < init) {
      throw new FilterException(
          ErrorDefinition.RANGE_INVALID_VALUES, new Object[] {rangeHeader}, HttpStatus.BAD_REQUEST);
    }
    int limit = finish - init + 1;
    return new Range(name, init, limit);
  }
}

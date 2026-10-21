package com.tychewealth.listing.domain;

import com.tychewealth.listing.enums.ErrorDefinition;
import com.tychewealth.listing.exception.FilterException;
import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * Offset, maximum result count, and ordering requested for a listing query.
 *
 * <p>Construction rejects negative offsets and limits below one so downstream paging code receives
 * usable values.
 */
public record PageCriteria(int offset, int limit, List<SortCriteria> sorts) {

  public PageCriteria {
    if (offset < 0) {
      throw new FilterException(ErrorDefinition.PAGINATION_INVALID_OFFSET, HttpStatus.BAD_REQUEST);
    }
    if (limit < 1) {
      throw new FilterException(ErrorDefinition.PAGINATION_INVALID_LIMIT, HttpStatus.BAD_REQUEST);
    }
  }
}

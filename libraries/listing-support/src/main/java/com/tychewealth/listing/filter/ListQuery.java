package com.tychewealth.listing.filter;

import com.tychewealth.listing.domain.ApiFilter;
import com.tychewealth.listing.domain.PageCriteria;
import java.util.List;

/**
 * All parsed criteria needed to execute one listing request.
 *
 * <p>{@code range} is populated only when paging came from a valid {@code Range} header; ordinary
 * {@code offset}/{@code limit} requests leave it null.
 */
public record ListQuery(List<ApiFilter> filter, PageCriteria pageCriteria, Range range) {

  public ListQuery(List<ApiFilter> filter, PageCriteria pageCriteria) {
    this(filter, pageCriteria, null);
  }
}

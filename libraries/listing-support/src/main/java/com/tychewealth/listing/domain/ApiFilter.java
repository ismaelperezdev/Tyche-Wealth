package com.tychewealth.listing.domain;

import com.tychewealth.listing.enums.FilterOperator;
import java.util.List;

/**
 * A filter operation requested for one API field.
 *
 * <p>Values remain strings at the HTTP boundary; a persistence adapter converts them to the mapped
 * property's type. Multiple values are retained for operators that support alternatives.
 */
public record ApiFilter(String field, FilterOperator operator, List<String> values) {

  public ApiFilter(String field, FilterOperator operator, String... values) {
    this(field, operator, values != null ? List.of(values) : List.of());
  }

  public boolean isMultiple() {
    return values.size() > 1;
  }

  public String singleValue() {
    if (values.isEmpty()) {
      return "";
    }

    return values.getFirst();
  }
}

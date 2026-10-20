package com.tychewealth.listing.shared;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Explicit boundary between client-visible filter keys and persistence query fields.
 *
 * <p>Override {@link #fieldNameMap()} to whitelist and translate API field names. Override {@link
 * #valueTransformers()} when request values need domain-specific conversion before the JPA adapter
 * converts them to the mapped field's Java type.
 */
public abstract class FilterFieldMapping {

  protected Map<String, String> fieldNameMap() {
    return Map.of();
  }

  protected Map<String, Function<String, Object>> valueTransformers() {
    return Map.of();
  }

  public final String resolveFieldName(String apiFieldName) {
    Objects.requireNonNull(apiFieldName, "apiFieldName must not be null");
    return fieldNameMap().getOrDefault(apiFieldName, apiFieldName);
  }

  public final boolean hasFieldMapping(String apiFieldName) {
    return fieldNameMap().containsKey(apiFieldName);
  }

  public final Object resolveValue(String apiFieldName, String value) {
    Objects.requireNonNull(apiFieldName, "apiFieldName must not be null");
    Function<String, Object> transformer = valueTransformers().get(apiFieldName);
    return transformer == null ? value : transformer.apply(value);
  }
}

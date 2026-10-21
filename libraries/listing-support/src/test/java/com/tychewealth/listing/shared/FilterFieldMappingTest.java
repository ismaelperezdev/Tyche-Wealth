package com.tychewealth.listing.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class FilterFieldMappingTest {

  private final FilterFieldMapping mapping =
      new FilterFieldMapping() {
        @Override
        protected Map<String, String> fieldNameMap() {
          return Map.of("created", "createdAt");
        }

        @Override
        protected Map<String, Function<String, Object>> valueTransformers() {
          return Map.of("active", Boolean::valueOf);
        }
      };

  @Test
  void resolvesConfiguredFieldAliasesAndLeavesUnknownFieldsUnchanged() {
    assertEquals("createdAt", mapping.resolveFieldName("created"));
    assertEquals("name", mapping.resolveFieldName("name"));
  }

  @Test
  void transformsConfiguredValuesAndLeavesUnknownValuesUnchanged() {
    assertEquals(Boolean.TRUE, mapping.resolveValue("active", "true"));
    assertEquals("anything", mapping.resolveValue("name", "anything"));
  }
}

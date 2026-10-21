package com.tychewealth.listing.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Projects serialized response JSON onto a set of requested property paths.
 *
 * <p>Exact path matches preserve the complete value; dotted paths recursively project nested
 * objects and array elements. Values that cannot contain a requested nested property are omitted.
 */
final class NestedPropertyFilter {

  private NestedPropertyFilter() {}

  static JsonNode project(JsonNode source, Set<String> requestedFields) {
    return projectNode(source, requestedFields);
  }

  private static JsonNode projectNode(JsonNode source, Set<String> requestedFields) {
    if (source.isObject()) {
      ObjectNode projected = JsonNodeFactory.instance.objectNode();
      source
          .properties()
          .forEach(
              entry -> {
                projectProperty(projected, entry.getKey(), entry.getValue(), requestedFields);
              });
      return projected;
    }

    if (source.isArray()) {
      return projectArray(source, requestedFields);
    }

    return requestedFields.isEmpty() ? source : null;
  }

  private static void projectProperty(
      ObjectNode projected, String property, JsonNode value, Set<String> requestedFields) {
    if (requestedFields.contains(property)) {
      projected.set(property, value);
      return;
    }

    Set<String> nested = nestedFields(requestedFields, property);
    if (!nested.isEmpty()) {
      JsonNode projectedNested = projectNode(value, nested);
      if (projectedNested != null) {
        projected.set(property, projectedNested);
      }
    }
  }

  private static ArrayNode projectArray(JsonNode source, Set<String> requestedFields) {
    ArrayNode projected = JsonNodeFactory.instance.arrayNode();
    source.forEach(
        element -> {
          JsonNode nested = projectNode(element, requestedFields);
          if (nested != null) {
            projected.add(nested);
          }
        });
    return projected;
  }

  private static Set<String> nestedFields(Set<String> requestedFields, String property) {
    String prefix = property + ".";
    Set<String> nestedFields = new LinkedHashSet<>();
    for (String field : requestedFields) {
      if (field.startsWith(prefix)) {
        nestedFields.add(field.substring(prefix.length()));
      }
    }
    return nestedFields;
  }
}

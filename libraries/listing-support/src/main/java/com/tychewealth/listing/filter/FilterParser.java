package com.tychewealth.listing.filter;

import com.tychewealth.listing.domain.ApiFilter;
import com.tychewealth.listing.domain.PageCriteria;
import com.tychewealth.listing.domain.SortCriteria;
import com.tychewealth.listing.dto.ExcludeFilter;
import com.tychewealth.listing.dto.Filterable;
import com.tychewealth.listing.enums.ErrorDefinition;
import com.tychewealth.listing.enums.FilterOperator;
import com.tychewealth.listing.exception.FilterException;
import com.tychewealth.listing.properties.FilterProperties;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Converts request parameters into validated filter, sort, and pagination criteria.
 *
 * <p>Filter and sort keys are checked against the response DTO's {@link Filterable} metadata;
 * reserved pagination and field-selection parameters are not treated as filters. DTO field metadata
 * is cached by class, while configured limits are applied for each parse operation.
 */
@Component
@Slf4j
public class FilterParser {

  private static final String OFFSET_PARAM = "offset";
  private static final String LIMIT_PARAM = "limit";
  private static final String SORT_PARAM = "sort";
  private static final String FIELDS_PARAM = "fields";
  private static final Set<String> RESERVED_PARAMS =
      Set.of(OFFSET_PARAM, LIMIT_PARAM, SORT_PARAM, FIELDS_PARAM);
  private static final int DEFAULT_OFFSET = 0;

  private static final Filterable ALL_OPERATORS =
      new Filterable() {
        @Override
        public FilterOperator[] operators() {
          return FilterOperator.values();
        }

        @Override
        public Class<? extends Annotation> annotationType() {
          return Filterable.class;
        }
      };

  private final Map<Class<?>, Map<String, Filterable>> filterableCache = new ConcurrentHashMap<>();

  private final FilterProperties filterProperties;

  public FilterParser(FilterProperties filterProperties) {
    this.filterProperties = filterProperties;
  }

  public ListQuery parse(Map<String, String> allRequestParams, Class<?> responseDtoClass) {
    return parse(allRequestParams, null, responseDtoClass);
  }

  public ListQuery parse(
      Map<String, String> allRequestParams, String requestRange, Class<?> responseDtoClass) {
    Objects.requireNonNull(responseDtoClass, "responseDtoClass must not be null");

    String offsetStr = allRequestParams.get(OFFSET_PARAM);
    String limitStr = allRequestParams.get(LIMIT_PARAM);

    Range range = null;

    if (offsetStr == null && limitStr == null && requestRange != null && !requestRange.isBlank()) {
      try {
        range = Range.parse(requestRange);
        offsetStr = String.valueOf(range.offset());
        limitStr = String.valueOf(range.limit());
      } catch (FilterException e) {
        log.warn("Invalid Range header '{}', using defaults {}", requestRange, e.getDescription());
      }
    }

    int offset = parseOffset(offsetStr);
    int limit = parseLimit(limitStr);
    if (range != null) {
      range = new Range(range.name(), offset, limit);
    }
    String sortRaw = allRequestParams.get(SORT_PARAM);

    List<SortCriteria> sorts = SortBuilder.parse(sortRaw, filterProperties.getMaxSorts());

    PageCriteria pageCriteria = new PageCriteria(offset, limit, sorts);

    Map<String, Filterable> filterableFields = getFilterableFields(responseDtoClass);
    for (SortCriteria sc : sorts) {
      if (!filterableFields.containsKey(sc.field())) {
        throw new FilterException(
            ErrorDefinition.FILTER_FIELD_NOT_FOUND,
            new Object[] {sc.field()},
            HttpStatus.BAD_REQUEST);
      }
    }

    Map<String, String> filterParams = new HashMap<>(allRequestParams);
    RESERVED_PARAMS.forEach(filterParams::remove);

    List<ApiFilter> filters = buildFilters(filterParams, responseDtoClass);

    return new ListQuery(filters, pageCriteria, range);
  }

  private int parseOffset(String offsetStr) {
    if (offsetStr == null || offsetStr.isBlank()) {
      return DEFAULT_OFFSET;
    }

    try {
      int offset = Integer.parseInt(offsetStr.trim());

      if (offset < 0) {
        throw new FilterException(
            ErrorDefinition.PAGINATION_INVALID_OFFSET, HttpStatus.BAD_REQUEST);
      }
      if (offset > filterProperties.getMaxOffset()) {
        log.warn(
            "Offset {} exceeds max {}, capping to {}",
            offset,
            filterProperties.getMaxOffset(),
            filterProperties.getMaxOffset());
        return filterProperties.getMaxOffset();
      }
      return offset;
    } catch (FilterException e) {
      throw e;
    } catch (NumberFormatException e) {
      throw new FilterException(
          ErrorDefinition.FILTER_PARSE_ERROR,
          new Object[] {offsetStr, "Integer"},
          HttpStatus.BAD_REQUEST);
    }
  }

  private int parseLimit(String limitStr) {
    if (limitStr == null || limitStr.isBlank()) {
      return filterProperties.getDefaultLimit();
    }

    try {
      int limit = Integer.parseInt(limitStr.trim());
      if (limit < 1) {
        throw new FilterException(ErrorDefinition.PAGINATION_INVALID_LIMIT, HttpStatus.BAD_REQUEST);
      }
      if (limit > filterProperties.getMaxLimit()) {
        log.warn(
            "Limit {} exceeds max {}, capping to {}",
            limit,
            filterProperties.getMaxLimit(),
            filterProperties.getMaxLimit());
        return filterProperties.getMaxLimit();
      }
      return limit;
    } catch (FilterException e) {
      throw e;
    } catch (NumberFormatException e) {
      throw new FilterException(
          ErrorDefinition.FILTER_PARSE_ERROR,
          new Object[] {limitStr, "Integer"},
          HttpStatus.BAD_REQUEST);
    }
  }

  private List<ApiFilter> buildFilters(Map<String, String> filterParams, Class<?> dtoClass) {
    if (filterParams.isEmpty()) {
      return Collections.emptyList();
    }

    if (filterParams.size() > filterProperties.getMaxFilters()) {
      throw new FilterException(
          ErrorDefinition.FILTER_MAX_EXCEEDED,
          new Object[] {filterProperties.getMaxFilters(), filterParams.size()},
          HttpStatus.BAD_REQUEST);
    }

    List<ApiFilter> filters = new ArrayList<>();
    Map<String, Filterable> filterableFields = getFilterableFields(dtoClass);

    for (Map.Entry<String, String> entry : filterParams.entrySet()) {
      String rawKey = entry.getKey();
      String rawValue = entry.getValue();

      ParsedKey parsedKey = parseKey(rawKey);
      String fieldName = parsedKey.fieldName();
      FilterOperator operator = parsedKey.operator();

      String effectiveValue = parsedKey.extractedValue();
      if (effectiveValue == null || effectiveValue.isBlank()) {
        effectiveValue = rawValue;
      }

      if (effectiveValue == null || effectiveValue.isBlank()) {
        continue;
      }

      Filterable annotation = filterableFields.get(fieldName);
      if (annotation == null) {
        throw new FilterException(
            ErrorDefinition.FILTER_FIELD_NOT_FOUND,
            new Object[] {fieldName},
            HttpStatus.BAD_REQUEST);
      }

      Set<FilterOperator> allowedOperators = new HashSet<>(Arrays.asList(annotation.operators()));
      if (!allowedOperators.contains(operator)) {
        throw new FilterException(
            ErrorDefinition.FILTER_OPERATOR_NOT_ALLOWED,
            new Object[] {operator.name(), fieldName},
            HttpStatus.BAD_REQUEST);
      }

      List<String> split =
          Arrays.stream(effectiveValue.split("[,;]"))
              .map(String::trim)
              .filter(s -> !s.isEmpty())
              .toList();
      ApiFilter apiFilter = new ApiFilter(fieldName, operator, split.toArray(new String[0]));
      filters.add(apiFilter);
    }

    return filters;
  }

  private Map<String, Filterable> computeFilterableFields(Class<?> dtoClass, int currentDepth) {
    Map<String, Filterable> result = new HashMap<>();
    Class<?> current = dtoClass;
    while (current != null && current != Object.class) {
      addFilterableFields(current, currentDepth, result);
      current = current.getSuperclass();
    }
    return Collections.unmodifiableMap(result);
  }

  private void addFilterableFields(
      Class<?> dtoClass, int currentDepth, Map<String, Filterable> result) {
    boolean classLevel = hasClassLevelFilterable(dtoClass);
    for (Field field : dtoClass.getDeclaredFields()) {
      if (field.isSynthetic()
          || Modifier.isStatic(field.getModifiers())
          || field.isAnnotationPresent(ExcludeFilter.class)) {
        continue;
      }
      addDirectFilterableField(field, classLevel, result);
      addNestedFilterableFields(field, currentDepth, result);
    }
  }

  private void addDirectFilterableField(
      Field field, boolean classLevel, Map<String, Filterable> result) {
    if (classLevel) {
      result.put(field.getName(), ALL_OPERATORS);
    }
    Filterable fieldAnnotation = field.getDeclaredAnnotation(Filterable.class);
    if (fieldAnnotation != null) {
      result.put(field.getName(), fieldAnnotation);
    }
  }

  private void addNestedFilterableFields(
      Field field, int currentDepth, Map<String, Filterable> result) {
    if (currentDepth >= filterProperties.getMaxNestedDepth()) {
      return;
    }

    Class<?> nestedDtoClass = extractedNestedDtoClass(field);
    if (nestedDtoClass == null || !hasClassLevelFilterable(nestedDtoClass)) {
      return;
    }

    try {
      Map<String, Filterable> nestedFields =
          computeFilterableFields(nestedDtoClass, currentDepth + 1);
      String prefix = field.getName() + ".";
      nestedFields.forEach((name, annotation) -> result.put(prefix + name, annotation));
    } catch (StackOverflowError exception) {
      log.warn(
          "Circular or too-deep nested reference detected for {}", nestedDtoClass.getSimpleName());
    }
  }

  private Map<String, Filterable> getFilterableFields(Class<?> dtoClass) {
    return filterableCache.computeIfAbsent(dtoClass, type -> computeFilterableFields(type, 0));
  }

  private boolean hasClassLevelFilterable(Class<?> clazz) {
    Class<?> current = clazz;
    while (current != null && current != Object.class) {
      if (current.isAnnotationPresent(Filterable.class)) {
        return true;
      }
      current = current.getSuperclass();
    }

    return false;
  }

  private ParsedKey parseKey(String rawKey) {

    int dotIndex = rawKey.lastIndexOf('.');
    if (dotIndex >= 0) {
      String suffix = rawKey.substring(dotIndex + 1).toLowerCase();
      FilterOperator operator = resolveOperator(suffix);
      if (operator != null) {
        return new ParsedKey(rawKey.substring(0, dotIndex), operator);
      }
    }

    String[][] urlOps = {{">=", "gte"}, {"<=", "lte"}, {"*=", "like"}, {">", "gt"}, {"<", "lt"}};

    for (String[] pair : urlOps) {
      String urlOp = pair[0];
      int idx = rawKey.indexOf(urlOp);
      if (idx > 0) {
        String field = rawKey.substring(0, idx);
        FilterOperator operator = resolveOperator((pair[1]));
        String value = rawKey.substring(idx + urlOp.length());
        return new ParsedKey(field, operator, value);
      }
    }

    return new ParsedKey(rawKey, FilterOperator.EQ);
  }

  private FilterOperator resolveOperator(String suffix) {
    return switch (suffix) {
      case "eq" -> FilterOperator.EQ;
      case "neq" -> FilterOperator.NEQ;
      case "gt" -> FilterOperator.GT;
      case "gte" -> FilterOperator.GTE;
      case "lt" -> FilterOperator.LT;
      case "lte" -> FilterOperator.LTE;
      case "like" -> FilterOperator.LIKE;
      case "in" -> FilterOperator.IN;
      default -> null;
    };
  }

  private Class<?> extractedNestedDtoClass(Field field) {
    Type genericType = field.getGenericType();
    if (genericType instanceof ParameterizedType pt) {
      Type[] args = pt.getActualTypeArguments();
      if (args.length > 0 && args[0] instanceof Class<?> clazz) {
        return clazz;
      }
    }

    return field.getType();
  }

  private record ParsedKey(String fieldName, FilterOperator operator, String extractedValue) {
    ParsedKey(String fieldName, FilterOperator operator) {
      this(fieldName, operator, null);
    }
  }
}

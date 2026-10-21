package com.tychewealth.listing.jpa;

import com.tychewealth.listing.domain.ApiFilter;
import com.tychewealth.listing.enums.ErrorDefinition;
import com.tychewealth.listing.enums.FilterOperator;
import com.tychewealth.listing.exception.FilterException;
import com.tychewealth.listing.shared.FilterFieldMapping;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * Translates parsed listing filters into composable Spring Data JPA specifications.
 *
 * <p>Only fields explicitly whitelisted by {@link FilterFieldMapping} are queryable. Values are
 * converted to the mapped entity property's Java type, and filter expressions are combined with AND
 * semantics; multiple values within equality and LIKE filters are alternatives.
 */
@Component
public class JpaListingSpecificationBuilder {

  private static final ConversionService CONVERSION_SERVICE = createConversionService();

  private static ConversionService createConversionService() {
    DefaultFormattingConversionService conversionService =
        new DefaultFormattingConversionService(false);
    DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
    registrar.setUseIsoFormat(true);
    registrar.registerFormatters(conversionService);
    return conversionService;
  }

  public <T> Specification<T> fromFilters(
      List<ApiFilter> filters, FilterFieldMapping fieldMapping) {
    if (filters == null || filters.isEmpty()) {
      return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
    }

    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>(filters.size());
      for (ApiFilter filter : filters) {
        if (!fieldMapping.hasFieldMapping(filter.field())) {
          throw fieldNotFound(filter.field());
        }
        Path<?> path = resolvePath(root, fieldMapping.resolveFieldName(filter.field()));
        List<Object> values = convertValues(filter, path.getJavaType(), fieldMapping);
        predicates.add(toPredicate(path, values, filter.operator(), criteriaBuilder));
      }
      return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }

  private Predicate toPredicate(
      Path<?> path, List<Object> values, FilterOperator operator, CriteriaBuilder criteriaBuilder) {
    return switch (operator) {
      case EQ -> combineComparisons(path, values, FilterOperator.EQ, false, criteriaBuilder);
      case NEQ -> combineComparisons(path, values, FilterOperator.NEQ, true, criteriaBuilder);
      case GT, GTE, LT, LTE -> {
        if (values.size() != 1) {
          throw invalidFilter("Comparison filters require exactly one value");
        }
        yield comparison(path, values.getFirst(), operator, criteriaBuilder);
      }
      case LIKE -> like(path, values, criteriaBuilder);
      case IN -> values.isEmpty() ? criteriaBuilder.disjunction() : path.in(values);
    };
  }

  private Predicate combineComparisons(
      Path<?> path,
      List<Object> values,
      FilterOperator operator,
      boolean requireAll,
      CriteriaBuilder criteriaBuilder) {
    Predicate[] predicates =
        values.stream()
            .map(value -> comparison(path, value, operator, criteriaBuilder))
            .toArray(Predicate[]::new);
    return requireAll ? criteriaBuilder.and(predicates) : criteriaBuilder.or(predicates);
  }

  private Predicate comparison(
      Path<?> path, Object value, FilterOperator operator, CriteriaBuilder criteriaBuilder) {
    return switch (operator) {
      case EQ -> value == null ? criteriaBuilder.isNull(path) : criteriaBuilder.equal(path, value);
      case NEQ ->
          value == null ? criteriaBuilder.isNotNull(path) : criteriaBuilder.notEqual(path, value);
      case GT, GTE, LT, LTE -> comparablePredicate(path, value, operator, criteriaBuilder);
      default -> throw invalidFilter("Unsupported comparison operator: " + operator);
    };
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Predicate comparablePredicate(
      Path<?> path, Object value, FilterOperator operator, CriteriaBuilder criteriaBuilder) {
    if (!(value instanceof Comparable comparable)) {
      throw invalidFilter("Range comparisons require a comparable field");
    }

    Expression expression = path;
    return switch (operator) {
      case GT -> criteriaBuilder.greaterThan(expression, comparable);
      case GTE -> criteriaBuilder.greaterThanOrEqualTo(expression, comparable);
      case LT -> criteriaBuilder.lessThan(expression, comparable);
      case LTE -> criteriaBuilder.lessThanOrEqualTo(expression, comparable);
      default -> throw invalidFilter("Unsupported range operator: " + operator);
    };
  }

  private Predicate like(Path<?> path, List<Object> values, CriteriaBuilder criteriaBuilder) {
    if (path.getJavaType() != String.class) {
      throw invalidFilter("LIKE filters require a string field");
    }
    if (values.isEmpty()) {
      return criteriaBuilder.disjunction();
    }

    Expression<String> expression = criteriaBuilder.lower(path.as(String.class));
    return criteriaBuilder.or(
        values.stream()
            .map(
                value ->
                    criteriaBuilder.like(
                        expression,
                        "%" + escapeLike(value.toString().toLowerCase(Locale.ROOT)) + "%",
                        '\\'))
            .toArray(Predicate[]::new));
  }

  private String escapeLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  private List<Object> convertValues(
      ApiFilter filter, Class<?> targetType, FilterFieldMapping fieldMapping) {
    return filter.values().stream()
        .map(value -> fieldMapping.resolveValue(filter.field(), value))
        .map(value -> convertValue(value, targetType, filter.field()))
        .toList();
  }

  private Object convertValue(Object value, Class<?> targetType, String fieldName) {
    if (value == null) {
      return null;
    }
    Class<?> boxedType = ClassUtils.resolvePrimitiveIfNecessary(targetType);
    if (boxedType.isInstance(value)) {
      return value;
    }

    String text = value.toString();
    try {
      if (boxedType == Boolean.class
          && !text.equalsIgnoreCase("true")
          && !text.equalsIgnoreCase("false")) {
        throw new IllegalArgumentException("Invalid boolean value");
      }
      if (CONVERSION_SERVICE.canConvert(value.getClass(), boxedType)) {
        return CONVERSION_SERVICE.convert(value, boxedType);
      }
    } catch (ConversionException | IllegalArgumentException exception) {
      throw new FilterException(
          ErrorDefinition.FILTER_PARSE_ERROR,
          new Object[] {text, boxedType.getSimpleName()},
          HttpStatus.BAD_REQUEST);
    }

    throw invalidFilter(
        "No filter value converter is available for field " + fieldName + " of type " + boxedType);
  }

  private Path<?> resolvePath(Root<?> root, String fieldPath) {
    Path<?> path = root;
    for (String segment : fieldPath.split("\\.")) {
      path = path.get(segment);
    }
    return path;
  }

  private FilterException fieldNotFound(String fieldName) {
    return new FilterException(
        ErrorDefinition.FILTER_FIELD_NOT_FOUND, new Object[] {fieldName}, HttpStatus.BAD_REQUEST);
  }

  private FilterException invalidFilter(String message) {
    return new FilterException(
        ErrorDefinition.FILTER_PARSE_ERROR,
        java.util.Map.of("reason", message),
        HttpStatus.BAD_REQUEST);
  }
}

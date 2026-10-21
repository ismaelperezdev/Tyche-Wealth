package com.tychewealth.listing.dto;

import com.tychewealth.listing.enums.FilterOperator;
import java.lang.annotation.*;

/**
 * Declares which response DTO fields may be named by listing filters and sort expressions.
 *
 * <p>On a type, fields are filterable by default with all operators. On an individual field, {@link
 * #operators()} restricts accepted operations and overrides the type-level default.
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Filterable {

  FilterOperator[] operators() default FilterOperator.EQ;
}

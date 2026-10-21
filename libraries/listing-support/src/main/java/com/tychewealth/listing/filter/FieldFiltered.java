package com.tychewealth.listing.filter;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonFilter;
import java.lang.annotation.*;

/**
 * Adds the Jackson filter identifier required for {@link FieldFilterAdvice} field projection.
 *
 * <p>Place this annotation on a response type (or composed annotation) whose properties may be
 * selected through the request's {@code fields} parameter.
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@JacksonAnnotationsInside
@JsonFilter(FieldFiltered.FILTER_ID)
public @interface FieldFiltered {

  String FILTER_ID = "fieldFilter";
}

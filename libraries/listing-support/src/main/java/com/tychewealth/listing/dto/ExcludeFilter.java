package com.tychewealth.listing.dto;

import java.lang.annotation.*;

/**
 * Opts a field out of type-level {@link Filterable} defaults.
 *
 * <p>The field remains part of the response DTO but is not accepted as a filter or sort key.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcludeFilter {}

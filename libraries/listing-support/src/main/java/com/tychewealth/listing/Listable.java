package com.tychewealth.listing;

import com.tychewealth.listing.shared.FilterFieldMapping;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables listing query parsing for one Spring MVC handler method.
 *
 * <p>The configured mapping translates public filter names to application or persistence fields.
 * Filterable fields are derived from the response DTO declared by the handler method.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Listable {

  Class<? extends FilterFieldMapping> mapping();
}

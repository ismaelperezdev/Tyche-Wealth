package com.tychewealth.listing.properties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Runtime limits for listing request parsing, bound from the {@code filter} configuration prefix.
 *
 * <p>The properties cap client-controlled offsets, result sizes, filter counts, nested filter
 * depth, and sort expressions. {@code defaultLimit} applies when a request omits its limit.
 */
@ConfigurationProperties(prefix = "filter")
@Getter
@Setter
@NoArgsConstructor
public class FilterProperties {

  private int maxOffset = 100000;
  private int maxLimit = 5000;
  private int maxFilters = 20;
  private int defaultLimit = 20;
  private int maxNestedDepth = 2;
  private int maxSorts = 10;
}

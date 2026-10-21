package com.tychewealth.listing;

import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.tychewealth.listing.filter.FieldFiltered;
import com.tychewealth.listing.filter.ListQueryArgumentResolver;
import com.tychewealth.listing.properties.FilterProperties;
import java.util.List;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Integrates listing support with Spring MVC.
 *
 * <p>Binds the {@code filter.*} settings and registers the argument resolver that supplies parsed
 * {@link com.tychewealth.listing.filter.ListQuery} values to {@link Listable} handlers.
 */
@Configuration
@EnableConfigurationProperties(FilterProperties.class)
public class ListingSupportConfiguration implements WebMvcConfigurer {

  private final ListQueryArgumentResolver argumentResolver;

  public ListingSupportConfiguration(ListQueryArgumentResolver argumentResolver) {
    this.argumentResolver = argumentResolver;
  }

  @Bean
  public Jackson2ObjectMapperBuilderCustomizer listingFilterProviderCustomizer() {
    return builder ->
        builder.filters(
            new SimpleFilterProvider()
                .addFilter(FieldFiltered.FILTER_ID, SimpleBeanPropertyFilter.serializeAll())
                .setFailOnUnknownId(false));
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(argumentResolver);
  }
}

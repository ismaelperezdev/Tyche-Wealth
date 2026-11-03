package com.tychewealth;

import com.tychewealth.listing.ListingSupportConfiguration;
import com.tychewealth.listing.filter.FilterParser;
import com.tychewealth.listing.filter.ListQueryArgumentResolver;
import com.tychewealth.listing.jpa.JpaListingSpecificationBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@Import({
  ListingSupportConfiguration.class,
  ListQueryArgumentResolver.class,
  FilterParser.class,
  JpaListingSpecificationBuilder.class
})
public class TycheWealthMarketDataServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(TycheWealthMarketDataServiceApplication.class, args);
  }
}

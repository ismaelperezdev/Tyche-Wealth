package com.tychewealth.controller;

import static com.tychewealth.constants.TestConstants.LATEST_PRICES_ENDPOINT;
import static com.tychewealth.constants.TestConstants.PRICE_HISTORY_ENDPOINT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:market-price-security;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.liquibase.enabled=false",
      "kafka.enabled=false",
      "app.market-price.scheduler.enabled=false"
    })
@AutoConfigureMockMvc
class MarketPriceSecurityIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @ParameterizedTest
  @ValueSource(strings = {LATEST_PRICES_ENDPOINT, PRICE_HISTORY_ENDPOINT})
  void rejectsUnauthenticatedRequestsToInternalPriceRoutes(String endpoint) throws Exception {
    mockMvc
        .perform(get(endpoint).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }
}

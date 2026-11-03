package com.tychewealth.controller;

import static com.tychewealth.constants.TestConstants.SECOND_TEST_SYMBOL;
import static com.tychewealth.constants.TestConstants.TEST_SYMBOL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tychewealth.entity.MarketPriceCheckpointEntity;
import com.tychewealth.entity.MarketSymbolEntity;
import com.tychewealth.error.ErrorDefinition;
import com.tychewealth.model.MarketQuote;
import com.tychewealth.repository.MarketPriceCheckpointRepository;
import com.tychewealth.repository.MarketSymbolRepository;
import com.tychewealth.store.MarketQuoteStore;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.hamcrest.Matchers;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:market-price-api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.properties.hibernate.generate_statistics=true",
      "spring.jpa.properties.hibernate.session.events.log=false",
      "spring.liquibase.enabled=false",
      "kafka.enabled=false",
      "app.market-price.scheduler.enabled=false"
    })
@AutoConfigureMockMvc(addFilters = false)
class MarketPriceApiIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private MarketQuoteStore marketQuoteStore;

  @Autowired private MarketPriceCheckpointRepository checkpointRepository;

  @Autowired private MarketSymbolRepository symbolRepository;

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  void pagesLatestPricesBeforeLookingUpQuotesAndReturnsPaginationHeaders() throws Exception {
    when(marketQuoteStore.findBySymbols(List.of(SECOND_TEST_SYMBOL)))
        .thenReturn(
            List.of(
                new MarketQuote(
                    SECOND_TEST_SYMBOL,
                    new BigDecimal("412.50"),
                    LocalDateTime.of(2026, 1, 2, 12, 0))));

    mockMvc
        .perform(
            get("/internal/prices/latest")
                .param("symbol.in", TEST_SYMBOL + "," + SECOND_TEST_SYMBOL)
                .param("offset", "1")
                .param("limit", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].symbol").value(SECOND_TEST_SYMBOL))
        .andExpect(jsonPath("$[0].price").value(412.50))
        .andExpect(header().string("X-Total-Count", "2"))
        .andExpect(header().string("Link", Matchers.containsString("rel=\"prev\"")));

    verify(marketQuoteStore).findBySymbols(List.of(SECOND_TEST_SYMBOL));
    verifyNoMoreInteractions(marketQuoteStore);
  }

  @Test
  void preservesCandidateCountWhenSomeRequestedSymbolsHaveNoQuote() throws Exception {
    when(marketQuoteStore.findBySymbols(List.of(TEST_SYMBOL, SECOND_TEST_SYMBOL)))
        .thenReturn(
            List.of(
                new MarketQuote(
                    TEST_SYMBOL, new BigDecimal("187.24"), LocalDateTime.of(2026, 1, 2, 12, 0))));

    mockMvc
        .perform(
            get("/internal/prices/latest")
                .param("symbol.in", TEST_SYMBOL + "," + SECOND_TEST_SYMBOL)
                .param("limit", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].symbol").value(TEST_SYMBOL))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(header().string("X-Total-Count", "2"));

    verify(marketQuoteStore).findBySymbols(List.of(TEST_SYMBOL, SECOND_TEST_SYMBOL));
    verifyNoMoreInteractions(marketQuoteStore);
  }

  @Test
  void rejectsLatestPriceRequestWithoutRequiredSymbolFilter() throws Exception {
    mockMvc
        .perform(get("/internal/prices/latest"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.code")
                .value(ErrorDefinition.MARKET_PRICE_SYMBOL_FILTER_REQUIRED.getCode()));

    verifyNoInteractions(marketQuoteStore);
  }

  @Test
  void rejectsMalformedHistoryTimestampFiltersAsBadRequests() throws Exception {
    mockMvc
        .perform(
            get("/internal/prices/history")
                .param("symbol.in", TEST_SYMBOL)
                .param("capturedAt.eq", "not-an-instant"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.code").value(ErrorDefinition.MARKET_PRICE_CAPTURED_AT_INVALID.getCode()));

    mockMvc
        .perform(
            get("/internal/prices/history")
                .param("symbol.in", TEST_SYMBOL)
                .param("bucketStart.gte", "not-an-instant"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.code").value(ErrorDefinition.MARKET_PRICE_CAPTURED_AT_INVALID.getCode()));
  }

  @Test
  void rejectsInstantOutsideLocalDateTimeRangeAsBadRequest() throws Exception {
    mockMvc
        .perform(
            get("/internal/prices/history")
                .param("symbol.in", TEST_SYMBOL)
                .param("capturedAt.eq", "+1000000000-12-31T23:59:59.999999999Z"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.code").value(ErrorDefinition.MARKET_PRICE_CAPTURED_AT_INVALID.getCode()));
  }

  @Test
  void fetchesCheckpointSymbolsWithThePagedQuery() throws Exception {
    MarketSymbolEntity firstSymbol = saveSymbol(TEST_SYMBOL);
    MarketSymbolEntity secondSymbol = saveSymbol(SECOND_TEST_SYMBOL);
    LocalDateTime capturedAt = LocalDateTime.of(2026, 1, 2, 12, 0);
    checkpointRepository.saveAllAndFlush(
        List.of(
            new MarketPriceCheckpointEntity(
                firstSymbol, new BigDecimal("187.24"), capturedAt, capturedAt),
            new MarketPriceCheckpointEntity(
                secondSymbol, new BigDecimal("412.50"), capturedAt, capturedAt)));

    Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.clear();

    mockMvc
        .perform(
            get("/internal/prices/history")
                .param("symbol.in", TEST_SYMBOL + "," + SECOND_TEST_SYMBOL)
                .param("limit", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));

    assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
  }

  private MarketSymbolEntity saveSymbol(String symbol) {
    MarketSymbolEntity entity = new MarketSymbolEntity();
    entity.setSymbol(symbol);
    entity.setActive(true);
    return symbolRepository.saveAndFlush(entity);
  }
}

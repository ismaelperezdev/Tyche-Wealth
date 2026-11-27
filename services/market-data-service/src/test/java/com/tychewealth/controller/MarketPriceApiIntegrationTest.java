package com.tychewealth.controller;

import static com.tychewealth.constants.TestConstants.LATEST_PRICES_ENDPOINT;
import static com.tychewealth.constants.TestConstants.PRICE_HISTORY_ENDPOINT;
import static com.tychewealth.constants.TestConstants.SECOND_TEST_SYMBOL;
import static com.tychewealth.constants.TestConstants.TEST_SYMBOL;
import static com.tychewealth.constants.TestConstants.TEST_SYMBOL_IN_FILTER;
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
import java.time.ZoneOffset;
import java.util.List;
import org.hamcrest.Matchers;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
      "app.market-price.scheduler.enabled=false",
      "app.market-price.history-scan-page-size=2"
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
            get(LATEST_PRICES_ENDPOINT)
                .param(TEST_SYMBOL_IN_FILTER, TEST_SYMBOL + "," + SECOND_TEST_SYMBOL)
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
            get(LATEST_PRICES_ENDPOINT)
                .param(TEST_SYMBOL_IN_FILTER, TEST_SYMBOL + "," + SECOND_TEST_SYMBOL)
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
        .perform(get(LATEST_PRICES_ENDPOINT))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.code")
                .value(ErrorDefinition.MARKET_PRICE_SYMBOL_FILTER_REQUIRED.getCode()));

    verifyNoInteractions(marketQuoteStore);
  }

  @Test
  void rejectsCheckpointRetrieveWithoutRequiredSymbolFilter() throws Exception {
    mockMvc
        .perform(get(PRICE_HISTORY_ENDPOINT))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.code").value(ErrorDefinition.MARKET_PRICE_SYMBOLS_REQUIRED.getCode()));
  }

  @Test
  void rejectsMalformedRetrieveTimestampAsBadRequest() throws Exception {
    mockMvc
        .perform(
            get(PRICE_HISTORY_ENDPOINT)
                .param(TEST_SYMBOL_IN_FILTER, TEST_SYMBOL)
                .param("capturedAt", "not-an-instant"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.code").value(ErrorDefinition.MARKET_PRICE_CAPTURED_AT_INVALID.getCode()));
  }

  @Test
  void rejectsMalformedSampledHistoryTimestampAsBadRequest() throws Exception {
    mockMvc
        .perform(
            get(PRICE_HISTORY_ENDPOINT + "/sampled")
                .param(TEST_SYMBOL_IN_FILTER, TEST_SYMBOL)
                .param("capturedAt.gte", "not-an-instant"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.code").value(ErrorDefinition.MARKET_PRICE_CAPTURED_AT_INVALID.getCode()));
  }

  @Test
  void rejectsSampledHistoryRequestWithoutRequiredStartAsBadRequest() throws Exception {
    mockMvc
        .perform(get(PRICE_HISTORY_ENDPOINT + "/sampled").param(TEST_SYMBOL_IN_FILTER, TEST_SYMBOL))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.code").value(ErrorDefinition.MARKET_PRICE_TIME_RANGE_INVALID.getCode()));
  }

  @Test
  void rejectsInstantOutsideLocalDateTimeRangeAsBadRequest() throws Exception {
    mockMvc
        .perform(
            get(PRICE_HISTORY_ENDPOINT)
                .param(TEST_SYMBOL_IN_FILTER, TEST_SYMBOL)
                .param("capturedAt", "+1000000000-12-31T23:59:59.999999999Z"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.code").value(ErrorDefinition.MARKET_PRICE_CAPTURED_AT_INVALID.getCode()));
  }

  @Test
  @Transactional
  void retrievesCheckpointForEachRequestedSymbolWithoutPagination() throws Exception {
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
            get(PRICE_HISTORY_ENDPOINT)
                .param(TEST_SYMBOL_IN_FILTER, TEST_SYMBOL + "," + SECOND_TEST_SYMBOL)
                .param("capturedAt", "2026-01-03T00:00:00Z"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));

    assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
  }

  @Test
  void samplesHistoryWithinDatabaseBatchesWhenResolutionIsRequested() throws Exception {
    String sampledSymbol = "SAMPLETEST";
    MarketSymbolEntity symbol = saveSymbol(sampledSymbol);
    checkpointRepository.saveAllAndFlush(
        List.of(
            new MarketPriceCheckpointEntity(
                symbol,
                new BigDecimal("187.24"),
                LocalDateTime.of(2026, 2, 2, 12, 0),
                LocalDateTime.of(2026, 2, 2, 12, 0)),
            new MarketPriceCheckpointEntity(
                symbol,
                new BigDecimal("188.24"),
                LocalDateTime.of(2026, 2, 25, 12, 0),
                LocalDateTime.of(2026, 2, 25, 12, 0))));

    mockMvc
        .perform(
            get(PRICE_HISTORY_ENDPOINT + "/sampled")
                .param(TEST_SYMBOL_IN_FILTER, sampledSymbol)
                .param("capturedAt.gte", "2025-02-01T00:00:00Z")
                .param("capturedAt.lte", "2026-03-01T00:00:00Z"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].capturedAt").value("2026-02-25T12:00:00Z"))
        .andExpect(header().doesNotExist("X-Total-Count"))
        .andExpect(header().doesNotExist("Link"));
  }

  @Test
  void retrievesLatestCheckpointForEachSymbolAtOrBeforeRequestedInstant() throws Exception {
    MarketSymbolEntity firstSymbol = saveSymbol("RETRIEVEA");
    MarketSymbolEntity secondSymbol = saveSymbol("RETRIEVEB");
    LocalDateTime earlier = LocalDateTime.of(2026, 2, 2, 12, 0);
    LocalDateTime later = earlier.plusDays(1);
    checkpointRepository.saveAllAndFlush(
        List.of(
            new MarketPriceCheckpointEntity(firstSymbol, new BigDecimal("100"), earlier, earlier),
            new MarketPriceCheckpointEntity(firstSymbol, new BigDecimal("110"), later, later),
            new MarketPriceCheckpointEntity(secondSymbol, new BigDecimal("200"), earlier, earlier),
            new MarketPriceCheckpointEntity(
                secondSymbol, new BigDecimal("210"), later.plusDays(2), later.plusDays(2))));

    mockMvc
        .perform(
            get(PRICE_HISTORY_ENDPOINT)
                .param(TEST_SYMBOL_IN_FILTER, "RETRIEVEA,RETRIEVEB")
                .param("capturedAt", "2026-02-04T00:00:00Z"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[?(@.symbol == 'RETRIEVEA')].price").value(110.0))
        .andExpect(jsonPath("$[?(@.symbol == 'RETRIEVEB')].price").value(200.0))
        .andExpect(jsonPath("$[0].requestedCapturedAt").value("2026-02-04T00:00:00Z"))
        .andExpect(jsonPath("$[1].requestedCapturedAt").value("2026-02-04T00:00:00Z"));
  }

  @Test
  void retrievesLatestCheckpointsForMultipleRequestedInstantsInOneCall() throws Exception {
    MarketSymbolEntity symbol = saveSymbol("BATCHASOF");
    LocalDateTime first = LocalDateTime.of(2026, 2, 2, 12, 0);
    LocalDateTime second = first.plusDays(1);
    checkpointRepository.saveAllAndFlush(
        List.of(
            new MarketPriceCheckpointEntity(symbol, new BigDecimal("100"), first, first),
            new MarketPriceCheckpointEntity(symbol, new BigDecimal("110"), second, second)));

    mockMvc
        .perform(
            get(PRICE_HISTORY_ENDPOINT)
                .param(TEST_SYMBOL_IN_FILTER, "BATCHASOF")
                .param("capturedAt", "2026-02-02T18:00:00Z", "2026-02-03T18:00:00Z"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].price").value(100))
        .andExpect(jsonPath("$[0].requestedCapturedAt").value("2026-02-02T18:00:00Z"))
        .andExpect(jsonPath("$[1].price").value(110))
        .andExpect(jsonPath("$[1].requestedCapturedAt").value("2026-02-03T18:00:00Z"));
  }

  @Test
  void retrievesCheckpointBeforeEachRequestedInstantWhenRequestsFallBetweenCaptures()
      throws Exception {
    MarketSymbolEntity symbol = saveSymbol("BETWEENCHECKPOINTS");
    LocalDateTime firstCheckpoint = LocalDateTime.of(2026, 2, 2, 12, 0);
    LocalDateTime secondCheckpoint = firstCheckpoint.plusDays(1);
    checkpointRepository.saveAllAndFlush(
        List.of(
            new MarketPriceCheckpointEntity(
                symbol, new BigDecimal("100"), firstCheckpoint, firstCheckpoint),
            new MarketPriceCheckpointEntity(
                symbol, new BigDecimal("110"), secondCheckpoint, secondCheckpoint)));

    mockMvc
        .perform(
            get(PRICE_HISTORY_ENDPOINT)
                .param(TEST_SYMBOL_IN_FILTER, "BETWEENCHECKPOINTS")
                .param("capturedAt", "2026-02-02T18:00:00Z", "2026-02-03T18:00:00Z"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].price").value(100))
        .andExpect(jsonPath("$[0].capturedAt").value("2026-02-02T12:00:00Z"))
        .andExpect(jsonPath("$[0].requestedCapturedAt").value("2026-02-02T18:00:00Z"))
        .andExpect(jsonPath("$[1].price").value(110))
        .andExpect(jsonPath("$[1].capturedAt").value("2026-02-03T12:00:00Z"))
        .andExpect(jsonPath("$[1].requestedCapturedAt").value("2026-02-03T18:00:00Z"));
  }

  @Test
  void retrievesLatestCheckpointWhenCapturedAtIsOmitted() throws Exception {
    String symbolName = "DEFAULTAT";
    MarketSymbolEntity symbol = saveSymbol(symbolName);
    LocalDateTime earlier = LocalDateTime.now(ZoneOffset.UTC).minusDays(2).withNano(0);
    LocalDateTime later = earlier.plusDays(1);
    checkpointRepository.saveAllAndFlush(
        List.of(
            new MarketPriceCheckpointEntity(symbol, new BigDecimal("100.00"), earlier, earlier),
            new MarketPriceCheckpointEntity(symbol, new BigDecimal("110.00"), later, later)));

    mockMvc
        .perform(get(PRICE_HISTORY_ENDPOINT).param(TEST_SYMBOL_IN_FILTER, symbolName))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].symbol").value(symbolName))
        .andExpect(jsonPath("$[0].price").value(110.00));
  }

  @Test
  void readsMultipleSampledBatchesWithoutExecutingCountQueries() throws Exception {
    MarketSymbolEntity symbol = saveSymbol("BATCHTEST");
    LocalDateTime start = LocalDateTime.of(2026, 2, 2, 0, 0);
    checkpointRepository.saveAllAndFlush(
        List.of(
            new MarketPriceCheckpointEntity(symbol, new BigDecimal("187.24"), start, start),
            new MarketPriceCheckpointEntity(
                symbol, new BigDecimal("188.24"), start.plusHours(6), start.plusHours(6)),
            new MarketPriceCheckpointEntity(
                symbol, new BigDecimal("189.24"), start.plusHours(12), start.plusHours(12))));
    Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.clear();

    mockMvc
        .perform(
            get(PRICE_HISTORY_ENDPOINT + "/sampled")
                .param(TEST_SYMBOL_IN_FILTER, "BATCHTEST")
                .param("capturedAt.gte", "2026-02-02T00:00:00Z")
                .param("capturedAt.lte", "2026-02-09T00:00:00Z"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3));

    assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
  }

  @Test
  @Transactional
  void repositoryScanSlicesRemainOrderedAcrossOffsets() {
    MarketSymbolEntity symbol = saveSymbol("ORDERTEST");
    LocalDateTime start = LocalDateTime.of(2026, 2, 2, 0, 0);
    checkpointRepository.saveAllAndFlush(
        List.of(
            new MarketPriceCheckpointEntity(
                symbol, new BigDecimal("189.24"), start.plusHours(12), start.plusHours(12)),
            new MarketPriceCheckpointEntity(symbol, new BigDecimal("187.24"), start, start),
            new MarketPriceCheckpointEntity(
                symbol, new BigDecimal("188.24"), start.plusHours(6), start.plusHours(6))));
    Specification<MarketPriceCheckpointEntity> specification =
        (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.join("marketSymbol").get("symbol"), "ORDERTEST");
    Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.clear();

    Slice<MarketPriceCheckpointEntity> firstSlice =
        checkpointRepository.findScanSlice(specification, PageRequest.of(0, 2));
    Slice<MarketPriceCheckpointEntity> secondSlice =
        checkpointRepository.findScanSlice(specification, PageRequest.of(1, 2));

    assertThat(firstSlice.getContent())
        .extracting(MarketPriceCheckpointEntity::getCapturedAt)
        .containsExactly(start, start.plusHours(6));
    assertThat(firstSlice.hasNext()).isTrue();
    assertThat(secondSlice.getContent())
        .extracting(MarketPriceCheckpointEntity::getCapturedAt)
        .containsExactly(start.plusHours(12));
    assertThat(secondSlice.hasNext()).isFalse();
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
  }

  private MarketSymbolEntity saveSymbol(String symbol) {
    MarketSymbolEntity entity = new MarketSymbolEntity();
    entity.setSymbol(symbol);
    entity.setActive(true);
    return symbolRepository.saveAndFlush(entity);
  }
}

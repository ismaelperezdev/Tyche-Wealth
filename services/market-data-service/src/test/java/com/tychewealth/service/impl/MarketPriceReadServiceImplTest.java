package com.tychewealth.service.impl;

import static com.tychewealth.constants.TestConstants.PADDED_TEST_SYMBOL;
import static com.tychewealth.constants.TestConstants.SECOND_TEST_SYMBOL;
import static com.tychewealth.constants.TestConstants.TEST_SYMBOL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tychewealth.entity.MarketPriceCheckpointEntity;
import com.tychewealth.entity.MarketSymbolEntity;
import com.tychewealth.error.ErrorDefinition;
import com.tychewealth.error.exception.InvalidMarketPriceQueryException;
import com.tychewealth.listing.domain.ApiFilter;
import com.tychewealth.listing.domain.PageCriteria;
import com.tychewealth.listing.domain.SortCriteria;
import com.tychewealth.listing.enums.FilterOperator;
import com.tychewealth.listing.enums.SortDirection;
import com.tychewealth.listing.filter.ListQuery;
import com.tychewealth.listing.jpa.JpaListingSpecificationBuilder;
import com.tychewealth.mapper.MarketPriceReadMapper;
import com.tychewealth.model.MarketQuote;
import com.tychewealth.service.MarketPriceCheckpointService;
import com.tychewealth.service.helper.MarketPriceFilterFieldMapping;
import com.tychewealth.service.helper.MarketPriceLatestFilterFieldMapping;
import com.tychewealth.service.helper.MarketPriceQueryValidationHelper;
import com.tychewealth.service.helper.MarketPriceReadHelper;
import com.tychewealth.store.MarketQuoteStore;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class MarketPriceReadServiceImplTest {

  private static final Instant FROM = Instant.parse("2026-08-27T12:00:00Z");
  private static final Instant TO = Instant.parse("2026-08-27T18:00:00Z");

  @Mock private MarketQuoteStore marketQuoteStore;
  @Mock private MarketPriceCheckpointService checkpointService;

  private MarketPriceReadServiceImpl service;

  @BeforeEach
  void setUp() {
    MarketPriceQueryValidationHelper validationHelper = new MarketPriceQueryValidationHelper();
    service =
        new MarketPriceReadServiceImpl(
            marketQuoteStore,
            checkpointService,
            new MarketPriceReadHelper(
                new JpaListingSpecificationBuilder(),
                new MarketPriceFilterFieldMapping(),
                new MarketPriceLatestFilterFieldMapping(),
                validationHelper),
            Mappers.getMapper(MarketPriceReadMapper.class));
  }

  @Test
  void returnsAvailableLatestPricesForNormalizedDistinctSymbols() {
    MarketQuote quote =
        new MarketQuote(
            TEST_SYMBOL, new BigDecimal("123.45"), LocalDateTime.of(2026, 8, 27, 12, 0));
    when(marketQuoteStore.findBySymbols(List.of(TEST_SYMBOL, SECOND_TEST_SYMBOL)))
        .thenReturn(List.of(quote));

    var prices =
        service.getLatestPrices(
            new ListQuery(
                List.of(
                    new ApiFilter(
                        "symbol",
                        FilterOperator.IN,
                        PADDED_TEST_SYMBOL,
                        TEST_SYMBOL,
                        SECOND_TEST_SYMBOL.toLowerCase())),
                new PageCriteria(0, 20, List.of())));

    assertThat(prices.content()).hasSize(1);
    assertThat(prices.content().getFirst().symbol()).isEqualTo(TEST_SYMBOL);
    assertThat(prices.content().getFirst().capturedAt()).isEqualTo(FROM);
    assertThat(prices.totalCandidates()).isEqualTo(2);
    verify(marketQuoteStore).findBySymbols(List.of(TEST_SYMBOL, SECOND_TEST_SYMBOL));
  }

  @Test
  void paginatesSymbolsBeforeReadingQuotesFromRedis() {
    when(marketQuoteStore.findBySymbols(List.of("GOOG"))).thenReturn(List.of());

    var prices =
        service.getLatestPrices(
            new ListQuery(
                List.of(new ApiFilter("symbol", FilterOperator.IN, "AAPL", "MSFT", "GOOG")),
                new PageCriteria(1, 1, List.of())));

    assertThat(prices.content()).isEmpty();
    assertThat(prices.totalCandidates()).isEqualTo(3);
    verify(marketQuoteStore).findBySymbols(List.of("GOOG"));
    verifyNoInteractions(checkpointService);
  }

  @Test
  void intersectsMultipleLatestSymbolFiltersBeforeReadingQuotes() {
    MarketQuote quote =
        new MarketQuote(
            SECOND_TEST_SYMBOL, new BigDecimal("123.45"), LocalDateTime.of(2026, 8, 27, 12, 0));
    when(marketQuoteStore.findBySymbols(List.of(SECOND_TEST_SYMBOL))).thenReturn(List.of(quote));

    var prices =
        service.getLatestPrices(
            new ListQuery(
                List.of(
                    new ApiFilter("symbol", FilterOperator.IN, TEST_SYMBOL, SECOND_TEST_SYMBOL),
                    new ApiFilter("symbol", FilterOperator.IN, SECOND_TEST_SYMBOL, "GOOG")),
                new PageCriteria(0, 20, List.of())));

    assertThat(prices.content()).hasSize(1);
    assertThat(prices.content().getFirst().symbol()).isEqualTo(SECOND_TEST_SYMBOL);
    assertThat(prices.totalCandidates()).isEqualTo(1);
    verify(marketQuoteStore).findBySymbols(List.of(SECOND_TEST_SYMBOL));
  }

  @Test
  void returnsEmptyLatestResultWhenSymbolFiltersAreDisjoint() {
    var prices =
        service.getLatestPrices(
            new ListQuery(
                List.of(
                    new ApiFilter("symbol", FilterOperator.EQ, TEST_SYMBOL),
                    new ApiFilter("symbol", FilterOperator.EQ, SECOND_TEST_SYMBOL)),
                new PageCriteria(0, 20, List.of())));

    assertThat(prices.content()).isEmpty();
    assertThat(prices.totalCandidates()).isZero();
    verify(marketQuoteStore).findBySymbols(List.of());
  }

  @Test
  void rejectsLatestPriceQueryWithoutSymbolFilter() {
    assertThatThrownBy(
            () ->
                service.getLatestPrices(
                    new ListQuery(List.of(), new PageCriteria(0, 20, List.of()))))
        .isInstanceOf(InvalidMarketPriceQueryException.class)
        .extracting(
            exception -> ((InvalidMarketPriceQueryException) exception).getErrorDefinition())
        .isEqualTo(ErrorDefinition.MARKET_PRICE_SYMBOL_FILTER_REQUIRED);

    verifyNoInteractions(marketQuoteStore);
  }

  @Test
  void returnsFilteredAndPagedHistoryAsFlatCheckpoints() {
    MarketSymbolEntity symbol = new MarketSymbolEntity();
    symbol.setSymbol("AAPL");
    LocalDateTime capturedAt = LocalDateTime.of(2026, 8, 27, 12, 30);
    LocalDateTime bucketStart = LocalDateTime.of(2026, 8, 27, 12, 0);
    MarketPriceCheckpointEntity checkpoint =
        new MarketPriceCheckpointEntity(symbol, new BigDecimal("123.45"), capturedAt, bucketStart);
    when(checkpointService.findCheckpoints(any(), any()))
        .thenReturn(new PageImpl<>(List.of(checkpoint)));

    var history =
        service.getPriceHistory(
            new ListQuery(
                List.of(
                    new ApiFilter("symbol", FilterOperator.IN, "aapl", "MSFT"),
                    new ApiFilter("capturedAt", FilterOperator.GTE, FROM.toString()),
                    new ApiFilter("capturedAt", FilterOperator.LTE, TO.toString())),
                new PageCriteria(0, 20, List.of())));

    assertThat(history.getContent()).hasSize(1);
    assertThat(history.getContent().getFirst().symbol()).isEqualTo("AAPL");
    assertThat(history.getContent().getFirst().capturedAt())
        .isEqualTo(Instant.parse("2026-08-27T12:30:00Z"));
    assertThat(history.getContent().getFirst().bucketStart()).isEqualTo(FROM);
    verify(checkpointService).findCheckpoints(any(), any());
  }

  @Test
  void appendsIdAsTieBreakerToRequestedHistorySort() {
    when(checkpointService.findCheckpoints(any(), any())).thenReturn(new PageImpl<>(List.of()));

    service.getPriceHistory(
        new ListQuery(
            List.of(new ApiFilter("symbol", FilterOperator.IN, "AAPL")),
            new PageCriteria(0, 20, List.of(new SortCriteria("capturedAt", SortDirection.DESC)))));

    verify(checkpointService)
        .findCheckpoints(
            any(),
            argThat(
                pageRequest ->
                    pageRequest.getSort().getOrderFor("capturedAt").isDescending()
                        && pageRequest.getSort().getOrderFor("id").isAscending()));
  }

  @Test
  void rejectsInvalidHistoryRangeBeforeQueryingCheckpoints() {
    assertThatThrownBy(
            () ->
                service.getPriceHistory(
                    new ListQuery(
                        List.of(
                            new ApiFilter("symbol", FilterOperator.IN, "AAPL"),
                            new ApiFilter("capturedAt", FilterOperator.GTE, TO.toString()),
                            new ApiFilter("capturedAt", FilterOperator.LTE, FROM.toString())),
                        new PageCriteria(0, 20, List.of()))))
        .isInstanceOf(InvalidMarketPriceQueryException.class);

    verifyNoInteractions(checkpointService);
  }

  @Test
  void allowsHistoryQueryWithRequiredSymbolAndNoTimeBounds() {
    when(checkpointService.findCheckpoints(any(), any())).thenReturn(new PageImpl<>(List.of()));

    var history =
        service.getPriceHistory(
            new ListQuery(
                List.of(new ApiFilter("symbol", FilterOperator.IN, "AAPL")),
                new PageCriteria(0, 20, List.of())));

    assertThat(history.getContent()).isEmpty();
    verify(checkpointService).findCheckpoints(any(), any());
  }

  @Test
  void allowsHistoryQueryWithOnlyOneTimeBound() {
    when(checkpointService.findCheckpoints(any(), any())).thenReturn(new PageImpl<>(List.of()));

    var history =
        service.getPriceHistory(
            new ListQuery(
                List.of(
                    new ApiFilter("symbol", FilterOperator.IN, "AAPL"),
                    new ApiFilter("capturedAt", FilterOperator.GTE, FROM.toString())),
                new PageCriteria(0, 20, List.of())));

    assertThat(history.getContent()).isEmpty();
    verify(checkpointService).findCheckpoints(any(), any());
  }

  @Test
  void rejectsHistoryQueryWithoutSymbolBeforeQueryingCheckpoints() {
    assertThatThrownBy(
            () ->
                service.getPriceHistory(
                    new ListQuery(List.of(), new PageCriteria(0, 20, List.of()))))
        .isInstanceOf(InvalidMarketPriceQueryException.class)
        .extracting(
            exception -> ((InvalidMarketPriceQueryException) exception).getErrorDefinition())
        .isEqualTo(ErrorDefinition.MARKET_PRICE_SYMBOL_FILTER_REQUIRED);

    verifyNoInteractions(checkpointService);
  }
}

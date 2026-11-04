package com.tychewealth.service;

import static com.tychewealth.constants.TestConstants.SECOND_TEST_SYMBOL;
import static com.tychewealth.constants.TestConstants.TEST_SYMBOL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tychewealth.entity.MarketPriceCheckpointEntity;
import com.tychewealth.entity.MarketSymbolEntity;
import com.tychewealth.model.MarketQuote;
import com.tychewealth.provider.MarketQuoteProvider;
import com.tychewealth.repository.MarketSymbolRepository;
import com.tychewealth.store.MarketQuoteStore;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketQuoteServiceTest {

  private static final LocalDateTime CAPTURED_AT = LocalDateTime.of(2026, 8, 27, 13, 15);

  @Mock private MarketSymbolRepository marketSymbolRepository;
  @Mock private MarketQuoteProvider marketQuoteProvider;
  @Mock private MarketQuoteStore marketQuoteStore;
  @Mock private MarketPriceCheckpointService marketPriceCheckpointService;
  @Mock private MarketSymbolEntity marketSymbol;

  private MarketQuoteService marketQuoteService;

  @BeforeEach
  void setUp() {
    marketQuoteService =
        new MarketQuoteService(
            marketSymbolRepository,
            marketQuoteProvider,
            marketQuoteStore,
            marketPriceCheckpointService);
  }

  @Test
  void shouldSaveQuoteAndNewCheckpointForActiveSymbol() {
    MarketQuote quote = new MarketQuote(TEST_SYMBOL, new BigDecimal("123.45"), CAPTURED_AT);
    when(marketSymbolRepository.findByActiveTrue()).thenReturn(List.of(marketSymbol));
    when(marketSymbol.getSymbol()).thenReturn(TEST_SYMBOL);
    when(marketQuoteProvider.fetchQuote(TEST_SYMBOL)).thenReturn(quote);
    marketQuoteService.captureQuotes();

    verify(marketQuoteStore).saveAll(List.of(quote));
    verify(marketPriceCheckpointService).saveNewCheckpoints(any());
  }

  @Test
  void shouldDoNothingWhenThereAreNoActiveSymbols() {
    when(marketSymbolRepository.findByActiveTrue()).thenReturn(List.of());

    marketQuoteService.captureQuotes();

    verifyNoInteractions(marketQuoteProvider, marketQuoteStore, marketPriceCheckpointService);
  }

  @Test
  void shouldCaptureAllActiveSymbolsBeforeDelegatingBatchPersistence() {
    MarketSymbolEntity secondSymbol = org.mockito.Mockito.mock(MarketSymbolEntity.class);
    MarketQuote firstQuote = new MarketQuote(TEST_SYMBOL, new BigDecimal("123.45"), CAPTURED_AT);
    MarketQuote secondQuote =
        new MarketQuote(SECOND_TEST_SYMBOL, new BigDecimal("234.56"), CAPTURED_AT);
    when(marketSymbolRepository.findByActiveTrue()).thenReturn(List.of(marketSymbol, secondSymbol));
    when(marketSymbol.getSymbol()).thenReturn(TEST_SYMBOL);
    when(secondSymbol.getSymbol()).thenReturn(SECOND_TEST_SYMBOL);
    when(marketQuoteProvider.fetchQuote(TEST_SYMBOL)).thenReturn(firstQuote);
    when(marketQuoteProvider.fetchQuote(SECOND_TEST_SYMBOL)).thenReturn(secondQuote);

    marketQuoteService.captureQuotes();

    verify(marketQuoteStore).saveAll(List.of(firstQuote, secondQuote));
    verify(marketPriceCheckpointService).saveNewCheckpoints(any());
  }

  @Test
  void shouldPropagateProviderFailureBeforeWritingAnyBatch() {
    when(marketSymbolRepository.findByActiveTrue()).thenReturn(List.of(marketSymbol));
    when(marketSymbol.getSymbol()).thenReturn(TEST_SYMBOL);
    when(marketQuoteProvider.fetchQuote(TEST_SYMBOL))
        .thenThrow(new IllegalStateException("provider failed"));

    assertThatThrownBy(() -> marketQuoteService.captureQuotes())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("provider failed");

    verifyNoInteractions(marketQuoteStore, marketPriceCheckpointService);
  }

  @Test
  void shouldDelegateQuotesFromDifferentBucketsSeparately() {
    MarketSymbolEntity secondSymbol = org.mockito.Mockito.mock(MarketSymbolEntity.class);
    MarketQuote firstQuote =
        new MarketQuote(TEST_SYMBOL, new BigDecimal("123.45"), LocalDateTime.of(2026, 8, 27, 5, 0));
    MarketQuote secondQuote =
        new MarketQuote(
            SECOND_TEST_SYMBOL, new BigDecimal("234.56"), LocalDateTime.of(2026, 8, 27, 7, 0));
    when(marketSymbolRepository.findByActiveTrue()).thenReturn(List.of(marketSymbol, secondSymbol));
    when(marketSymbol.getSymbol()).thenReturn(TEST_SYMBOL);
    when(secondSymbol.getSymbol()).thenReturn(SECOND_TEST_SYMBOL);
    when(marketQuoteProvider.fetchQuote(TEST_SYMBOL)).thenReturn(firstQuote);
    when(marketQuoteProvider.fetchQuote(SECOND_TEST_SYMBOL)).thenReturn(secondQuote);

    marketQuoteService.captureQuotes();

    ArgumentCaptor<Map<LocalDateTime, List<MarketPriceCheckpointEntity>>> captor =
        ArgumentCaptor.forClass(Map.class);
    verify(marketPriceCheckpointService).saveNewCheckpoints(captor.capture());
    assertThat(captor.getValue())
        .containsKeys(LocalDateTime.of(2026, 8, 27, 0, 0), LocalDateTime.of(2026, 8, 27, 6, 0));
  }
}

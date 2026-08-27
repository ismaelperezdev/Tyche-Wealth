package com.tychewealth.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.tychewealth.service.MarketQuoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketQuoteSchedulerTest {

  @Mock private MarketQuoteService marketQuoteService;

  private MarketQuoteScheduler marketQuoteScheduler;

  @BeforeEach
  void setUp() {
    marketQuoteScheduler = new MarketQuoteScheduler(marketQuoteService);
  }

  @Test
  void shouldTriggerQuoteCapture() {
    marketQuoteScheduler.captureQuotes();

    verify(marketQuoteService).captureQuotes();
  }

  @Test
  void shouldContainCaptureFailure() {
    doThrow(new IllegalStateException("capture failed")).when(marketQuoteService).captureQuotes();

    assertThatCode(() -> marketQuoteScheduler.captureQuotes()).doesNotThrowAnyException();
  }
}

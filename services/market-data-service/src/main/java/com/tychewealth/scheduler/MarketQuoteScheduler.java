package com.tychewealth.scheduler;

import static com.tychewealth.constants.LogConstants.MARKET_QUOTE_SCHEDULER;
import static com.tychewealth.constants.LogConstants.QUOTE_CAPTURE_ACTION;
import static com.tychewealth.constants.LogConstants.QUOTE_CAPTURE_FAILURE;
import static com.tychewealth.constants.LogConstants.QUOTE_CAPTURE_SUCCESS;

import com.tychewealth.service.MarketQuoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Triggers periodic market-quote capture. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    value = "app.market-price.scheduler.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class MarketQuoteScheduler {

  private final MarketQuoteService marketQuoteService;

  @Scheduled(
      fixedDelayString = "${app.market-price.scheduler.fixed-delay:5m}",
      initialDelayString = "${app.market-price.scheduler.initial-delay:0s}")
  public void captureQuotes() {
    long start = System.currentTimeMillis();

    try {
      marketQuoteService.captureQuotes();
      log.info(
          QUOTE_CAPTURE_SUCCESS,
          MARKET_QUOTE_SCHEDULER,
          QUOTE_CAPTURE_ACTION,
          System.currentTimeMillis() - start);
    } catch (RuntimeException error) {
      log.error(QUOTE_CAPTURE_FAILURE, MARKET_QUOTE_SCHEDULER, QUOTE_CAPTURE_ACTION, error);
    }
  }
}

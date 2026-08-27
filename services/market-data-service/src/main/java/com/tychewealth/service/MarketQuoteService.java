package com.tychewealth.service;

import com.tychewealth.entity.MarketPriceCheckpointEntity;
import com.tychewealth.entity.MarketSymbolEntity;
import com.tychewealth.model.MarketQuote;
import com.tychewealth.provider.MarketQuoteProvider;
import com.tychewealth.repository.MarketSymbolRepository;
import com.tychewealth.store.MarketQuoteStore;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Captures current quotes and persists their latest value and periodic checkpoints. */
@Service
@RequiredArgsConstructor
public class MarketQuoteService {

  private final MarketSymbolRepository marketSymbolRepository;
  private final MarketQuoteProvider marketQuoteProvider;
  private final MarketQuoteStore marketQuoteStore;
  private final MarketPriceCheckpointService marketPriceCheckpointService;

  @Value("${app.market-price.bucket-hours:6}")
  private int bucketHours = 6;

  @PostConstruct
  void validateBucketHours() {
    if (bucketHours <= 0 || 24 % bucketHours != 0) {
      throw new IllegalStateException(
          "app.market-price.bucket-hours must be a positive divisor of 24");
    }
  }

  public void captureQuotes() {
    Collection<MarketSymbolEntity> activeSymbols = marketSymbolRepository.findByActiveTrue();
    if (activeSymbols.isEmpty()) {
      return;
    }

    Map<LocalDateTime, List<MarketPriceCheckpointEntity>> checkpointsByBucket =
        new LinkedHashMap<>();
    List<MarketQuote> quotes = new ArrayList<>();

    for (MarketSymbolEntity marketSymbol : activeSymbols) {
      MarketQuote quote = marketQuoteProvider.fetchQuote(marketSymbol.getSymbol());
      LocalDateTime bucketStart = bucketStart(quote.capturedAt());
      quotes.add(quote);
      checkpointsByBucket
          .computeIfAbsent(bucketStart, ignored -> new ArrayList<>())
          .add(
              new MarketPriceCheckpointEntity(
                  marketSymbol, quote.price(), quote.capturedAt(), bucketStart));
    }

    marketQuoteStore.saveAll(quotes);
    marketPriceCheckpointService.saveNewCheckpoints(checkpointsByBucket);
  }

  private LocalDateTime bucketStart(LocalDateTime capturedAt) {
    int bucketHour = (capturedAt.getHour() / bucketHours) * bucketHours;
    return capturedAt.toLocalDate().atStartOfDay().plusHours(bucketHour);
  }
}

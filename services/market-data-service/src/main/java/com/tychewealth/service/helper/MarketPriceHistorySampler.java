package com.tychewealth.service.helper;

import com.tychewealth.entity.MarketPriceCheckpointEntity;
import com.tychewealth.model.HistoryResolution;
import com.tychewealth.service.MarketPriceCheckpointService;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/** Scans checkpoint history in bounded pages and retains the last real checkpoint per interval. */
@Component
public class MarketPriceHistorySampler {

  private final MarketPriceCheckpointService checkpointService;
  private final int scanPageSize;

  public MarketPriceHistorySampler(
      MarketPriceCheckpointService checkpointService,
      @Value("${app.market-price.history-scan-page-size:1000}") int scanPageSize) {
    if (scanPageSize < 1) {
      throw new IllegalArgumentException(
          "app.market-price.history-scan-page-size must be positive");
    }
    this.checkpointService = checkpointService;
    this.scanPageSize = scanPageSize;
  }

  public List<MarketPriceCheckpointEntity> sample(
      Specification<MarketPriceCheckpointEntity> specification, HistoryResolution resolution) {
    Map<BucketKey, MarketPriceCheckpointEntity> lastCheckpointByBucket = new LinkedHashMap<>();
    int pageNumber = 0;
    Slice<MarketPriceCheckpointEntity> slice;
    do {
      slice = checkpointService.findCheckpointScanSlice(specification, pageRequest(pageNumber));
      slice.getContent().stream()
          .forEach(
              checkpoint -> {
                BucketKey key =
                    new BucketKey(
                        checkpoint.getMarketSymbol().getSymbol(),
                        intervalStart(checkpoint, resolution));
                lastCheckpointByBucket.merge(
                    key, checkpoint, MarketPriceHistorySampler::laterCheckpoint);
              });
      pageNumber++;
    } while (slice.hasNext());

    return lastCheckpointByBucket.values().stream()
        .sorted(
            Comparator.comparing(MarketPriceCheckpointEntity::getCapturedAt)
                .thenComparing(MarketPriceCheckpointEntity::getId))
        .toList();
  }

  public HistoryResolution resolution(Instant from, Instant to) {
    LocalDateTime start = LocalDateTime.ofInstant(from, ZoneOffset.UTC);
    LocalDateTime end = LocalDateTime.ofInstant(to == null ? Instant.now() : to, ZoneOffset.UTC);

    if (!end.isAfter(start.plusWeeks(1))) {
      return HistoryResolution.SIX_HOURS;
    }
    if (!end.isAfter(start.plusMonths(1))) {
      return HistoryResolution.THREE_DAYS;
    }
    if (!end.isAfter(start.plusMonths(6))) {
      return HistoryResolution.WEEKLY;
    }
    if (!end.isAfter(start.plusYears(2))) {
      return HistoryResolution.MONTHLY;
    }
    return HistoryResolution.QUARTERLY;
  }

  private PageRequest pageRequest(int pageNumber) {
    return PageRequest.of(
        pageNumber, scanPageSize, Sort.by(Sort.Order.asc("capturedAt"), Sort.Order.asc("id")));
  }

  private static MarketPriceCheckpointEntity laterCheckpoint(
      MarketPriceCheckpointEntity candidate, MarketPriceCheckpointEntity current) {
    int timestampComparison = candidate.getCapturedAt().compareTo(current.getCapturedAt());
    return timestampComparison > 0
            || (timestampComparison == 0 && candidate.getId() > current.getId())
        ? candidate
        : current;
  }

  private static LocalDateTime intervalStart(
      MarketPriceCheckpointEntity checkpoint, HistoryResolution resolution) {
    LocalDateTime capturedAt = checkpoint.getCapturedAt();
    return switch (resolution) {
      case SIX_HOURS -> checkpoint.getBucketStart();
      case THREE_DAYS ->
          capturedAt
              .toLocalDate()
              .withDayOfMonth(1)
              .atStartOfDay()
              .plusDays(((capturedAt.getDayOfMonth() - 1) / 3) * 3L);
      case WEEKLY ->
          capturedAt
              .toLocalDate()
              .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
              .atStartOfDay();
      case MONTHLY -> capturedAt.toLocalDate().withDayOfMonth(1).atStartOfDay();
      case QUARTERLY ->
          capturedAt
              .toLocalDate()
              .withMonth(((capturedAt.getMonthValue() - 1) / 3) * 3 + 1)
              .withDayOfMonth(1)
              .atStartOfDay();
    };
  }

  private record BucketKey(String symbol, LocalDateTime intervalStart) {}
}

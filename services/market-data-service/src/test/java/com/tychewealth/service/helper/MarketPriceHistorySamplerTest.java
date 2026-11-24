package com.tychewealth.service.helper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.entity.MarketPriceCheckpointEntity;
import com.tychewealth.entity.MarketSymbolEntity;
import com.tychewealth.model.HistoryResolution;
import com.tychewealth.service.MarketPriceCheckpointService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

class MarketPriceHistorySamplerTest {

  private final MarketPriceCheckpointService checkpointService =
      mock(MarketPriceCheckpointService.class);
  private final MarketPriceHistorySampler sampler =
      new MarketPriceHistorySampler(checkpointService, 100);

  @Test
  void choosesResolutionAtRequestedRangeBoundaries() {
    Instant from = Instant.parse("2024-01-01T00:00:00Z");

    assertThat(sampler.resolution(from, from.plusSeconds(7 * 24 * 60 * 60)))
        .isEqualTo(HistoryResolution.SIX_HOURS);
    assertThat(sampler.resolution(from, from.atZone(ZoneOffset.UTC).plusMonths(1).toInstant()))
        .isEqualTo(HistoryResolution.THREE_DAYS);
    assertThat(sampler.resolution(from, from.atZone(ZoneOffset.UTC).plusMonths(6).toInstant()))
        .isEqualTo(HistoryResolution.WEEKLY);
    assertThat(sampler.resolution(from, from.atZone(ZoneOffset.UTC).plusYears(2).toInstant()))
        .isEqualTo(HistoryResolution.MONTHLY);
    assertThat(
            sampler.resolution(
                from, from.atZone(ZoneOffset.UTC).plusYears(2).plusNanos(1).toInstant()))
        .isEqualTo(HistoryResolution.QUARTERLY);
  }

  @Test
  void keepsLastRealCheckpointForEachSymbolAndTimeBucket() {
    MarketSymbolEntity symbol = new MarketSymbolEntity();
    symbol.setSymbol("AAPL");
    MarketPriceCheckpointEntity monday =
        checkpoint(symbol, "2026-08-24T06:00:00", "2026-08-24T06:00:00");
    MarketPriceCheckpointEntity thursday =
        checkpoint(symbol, "2026-08-27T12:00:00", "2026-08-27T12:00:00");
    MarketPriceCheckpointEntity nextMonday =
        checkpoint(symbol, "2026-08-31T06:00:00", "2026-08-31T06:00:00");
    when(checkpointService.findCheckpointScanSlice(any(), any()))
        .thenReturn(new SliceImpl<>(List.of(monday, thursday), PageRequest.of(0, 2), true))
        .thenReturn(new SliceImpl<>(List.of(nextMonday), PageRequest.of(1, 2), false));

    List<MarketPriceCheckpointEntity> sampled =
        new MarketPriceHistorySampler(checkpointService, 2).sample(null, HistoryResolution.WEEKLY);

    assertThat(sampled).containsExactly(thursday, nextMonday);
    verify(checkpointService, org.mockito.Mockito.times(2)).findCheckpointScanSlice(any(), any());
  }

  @Test
  void returnsEmptyWhenNoCheckpointsMatch() {
    when(checkpointService.findCheckpointScanSlice(any(), any()))
        .thenReturn(new SliceImpl<>(List.of()));

    assertThat(sampler.sample(null, HistoryResolution.MONTHLY)).isEmpty();

    verify(checkpointService).findCheckpointScanSlice(any(), any());
  }

  private static MarketPriceCheckpointEntity checkpoint(
      MarketSymbolEntity symbol, String capturedAt, String bucketStart) {
    return new MarketPriceCheckpointEntity(
        symbol,
        new BigDecimal("123.45"),
        LocalDateTime.parse(capturedAt),
        LocalDateTime.parse(bucketStart));
  }
}

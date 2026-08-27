package com.tychewealth.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tychewealth.entity.MarketPriceCheckpointEntity;
import com.tychewealth.entity.MarketSymbolEntity;
import com.tychewealth.mapper.MarketPriceCheckpointMapper;
import com.tychewealth.model.MarketPriceCheckpointBatch;
import com.tychewealth.repository.MarketPriceCheckpointRepository;
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
class MarketPriceCheckpointServiceTest {

  private static final Long MARKET_SYMBOL_ID = 1L;
  private static final LocalDateTime BUCKET_START = LocalDateTime.of(2026, 8, 27, 12, 0);

  @Mock private MarketPriceCheckpointRepository repository;
  private final MarketPriceCheckpointMapper mapper =
      org.mapstruct.factory.Mappers.getMapper(MarketPriceCheckpointMapper.class);
  @Mock private MarketPriceCheckpointEntity checkpoint;
  @Mock private MarketSymbolEntity marketSymbol;

  private MarketPriceCheckpointService service;

  @BeforeEach
  void setUp() {
    service = new MarketPriceCheckpointService(repository, mapper);
  }

  @Test
  void shouldPersistOnlyCheckpointsMissingForBucket() {
    stubCheckpoint(checkpoint);
    when(checkpoint.getMarketSymbol()).thenReturn(marketSymbol);
    when(marketSymbol.getId()).thenReturn(MARKET_SYMBOL_ID);
    service.saveNewCheckpoints(Map.of(BUCKET_START, List.of(checkpoint)));

    ArgumentCaptor<MarketPriceCheckpointBatch> batch =
        ArgumentCaptor.forClass(MarketPriceCheckpointBatch.class);
    verify(repository).insertIfAbsentBatch(batch.capture());

    org.assertj.core.api.Assertions.assertThat(batch.getValue().marketSymbolIds())
        .containsExactly("1");
    org.assertj.core.api.Assertions.assertThat(batch.getValue().prices()).containsExactly("10");
    org.assertj.core.api.Assertions.assertThat(batch.getValue().capturedAts())
        .containsExactly("2026-08-27 12:01");
    org.assertj.core.api.Assertions.assertThat(batch.getValue().bucketStarts())
        .containsExactly("2026-08-27 12:00");
  }

  @Test
  void shouldNotPersistCheckpointAlreadyPresentForBucket() {
    stubCheckpoint(checkpoint);
    when(checkpoint.getMarketSymbol()).thenReturn(marketSymbol);
    when(marketSymbol.getId()).thenReturn(MARKET_SYMBOL_ID);
    service.saveNewCheckpoints(Map.of(BUCKET_START, List.of(checkpoint)));

    verify(repository).insertIfAbsentBatch(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void shouldPersistOnlyMissingCheckpointsWhenBucketContainsExistingAndNewSymbols() {
    stubCheckpoint(checkpoint);
    when(checkpoint.getMarketSymbol()).thenReturn(marketSymbol);
    when(marketSymbol.getId()).thenReturn(MARKET_SYMBOL_ID);
    MarketPriceCheckpointEntity newCheckpoint = checkpointFor(2L);
    service.saveNewCheckpoints(Map.of(BUCKET_START, List.of(checkpoint, newCheckpoint)));

    verify(repository).insertIfAbsentBatch(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void shouldProcessMultipleBucketsIndependently() {
    stubCheckpoint(checkpoint);
    when(checkpoint.getMarketSymbol()).thenReturn(marketSymbol);
    when(marketSymbol.getId()).thenReturn(MARKET_SYMBOL_ID);
    LocalDateTime secondBucket = BUCKET_START.plusHours(6);
    MarketPriceCheckpointEntity secondCheckpoint = checkpointFor(2L);
    service.saveNewCheckpoints(
        Map.of(BUCKET_START, List.of(checkpoint), secondBucket, List.of(secondCheckpoint)));

    verify(repository, times(2)).insertIfAbsentBatch(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void shouldSplitCheckpointPersistenceAtBatchLimit() {
    List<MarketPriceCheckpointEntity> checkpoints =
        java.util.stream.IntStream.rangeClosed(1, 501)
            .mapToObj(index -> checkpointFor((long) index))
            .toList();
    service.saveNewCheckpoints(Map.of(BUCKET_START, checkpoints));

    verify(repository, times(2)).insertIfAbsentBatch(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void shouldNotAccessRepositoryForEmptyCheckpointMap() {
    service.saveNewCheckpoints(Map.of());

    verifyNoInteractions(repository);
  }

  private MarketPriceCheckpointEntity checkpointFor(Long symbolId) {
    MarketSymbolEntity symbol = org.mockito.Mockito.mock(MarketSymbolEntity.class);
    MarketPriceCheckpointEntity checkpointMock =
        org.mockito.Mockito.mock(MarketPriceCheckpointEntity.class);
    when(checkpointMock.getMarketSymbol()).thenReturn(symbol);
    when(symbol.getId()).thenReturn(symbolId);
    stubCheckpoint(checkpointMock);
    return checkpointMock;
  }

  private void stubCheckpoint(MarketPriceCheckpointEntity checkpoint) {
    when(checkpoint.getPrice()).thenReturn(BigDecimal.TEN);
    when(checkpoint.getCapturedAt()).thenReturn(BUCKET_START.plusMinutes(1));
    when(checkpoint.getBucketStart()).thenReturn(BUCKET_START);
  }
}

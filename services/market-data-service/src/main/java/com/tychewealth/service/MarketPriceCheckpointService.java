package com.tychewealth.service;

import com.tychewealth.entity.MarketPriceCheckpointEntity;
import com.tychewealth.mapper.MarketPriceCheckpointMapper;
import com.tychewealth.model.MarketPriceCheckpointBatch;
import com.tychewealth.repository.MarketPriceCheckpointRepository;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persists new market-price checkpoints within a database transaction. */
@Service
@RequiredArgsConstructor
public class MarketPriceCheckpointService {

  private final MarketPriceCheckpointRepository marketPriceCheckpointRepository;
  private final MarketPriceCheckpointMapper marketPriceCheckpointMapper;

  @Value("${app.market-price.checkpoint-batch-size:500}")
  private int checkpointBatchSize = 500;

  @PostConstruct
  void validateCheckpointBatchSize() {
    if (checkpointBatchSize <= 0) {
      throw new IllegalStateException(
          "app.market-price.checkpoint-batch-size must be greater than zero");
    }
  }

  @Transactional
  public void saveNewCheckpoints(
      Map<LocalDateTime, List<MarketPriceCheckpointEntity>> checkpointsByBucket) {
    checkpointsByBucket.values().forEach(this::saveNewCheckpointsForBucket);
  }

  private void saveNewCheckpointsForBucket(List<MarketPriceCheckpointEntity> checkpoints) {
    IntStream.iterate(0, start -> start < checkpoints.size(), start -> start + checkpointBatchSize)
        .mapToObj(
            start ->
                checkpoints.subList(
                    start, Math.min(start + checkpointBatchSize, checkpoints.size())))
        .forEach(this::saveCheckpointBatch);
  }

  private void saveCheckpointBatch(List<MarketPriceCheckpointEntity> checkpoints) {
    MarketPriceCheckpointBatch batch = marketPriceCheckpointMapper.toBatch(checkpoints);

    marketPriceCheckpointRepository.insertIfAbsentBatch(batch);
  }
}

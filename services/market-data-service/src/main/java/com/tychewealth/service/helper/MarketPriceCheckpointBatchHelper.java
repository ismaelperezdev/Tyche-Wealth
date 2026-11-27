package com.tychewealth.service.helper;

import com.tychewealth.dto.marketprice.PriceCheckpointDto;
import com.tychewealth.error.ErrorDefinition;
import com.tychewealth.error.exception.InvalidMarketPriceQueryException;
import com.tychewealth.service.MarketPriceCheckpointService;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves the latest checkpoint for each symbol at multiple requested instants.
 *
 * <p>The database resolves the latest checkpoint per symbol and instant without loading the
 * intervening checkpoint history into application memory.
 */
@Component
@RequiredArgsConstructor
public class MarketPriceCheckpointBatchHelper {

  private final MarketPriceCheckpointService checkpointService;

  /**
   * Returns each symbol's most recent checkpoint at or before every requested instant.
   *
   * <p>Results are ordered by requested instant and then by the input symbol order. Duplicate
   * requested instants are evaluated once. Symbols with no checkpoint at or before an instant are
   * omitted for that instant.
   *
   * @param symbols normalized symbols in the order required for the response
   * @param requestedCapturedAts instants for which the latest checkpoint is requested
   * @return checkpoints associated with their requested instants
   * @throws InvalidMarketPriceQueryException if no instants are supplied or an instant cannot be
   *     represented as a UTC local date-time
   */
  public List<PriceCheckpointDto> retrieveLatestPriceCheckpoints(
      List<String> symbols, List<Instant> requestedCapturedAts) {
    RequestedInstants instants = requestedInstants(requestedCapturedAts);
    return checkpointService.findLatestBySymbolsAtInstants(symbols, instants.orderedUtc()).stream()
        .map(
            checkpoint ->
                new PriceCheckpointDto(
                    checkpoint.symbol(),
                    checkpoint.price(),
                    checkpoint.capturedAt().toInstant(ZoneOffset.UTC),
                    checkpoint.bucketStart().toInstant(ZoneOffset.UTC),
                    instants.ordered().get((int) checkpoint.requestedIndex() - 1)))
        .toList();
  }

  /** Sorts and deduplicates requested instants while preserving their UTC query values. */
  private RequestedInstants requestedInstants(List<Instant> capturedAts) {
    if (capturedAts == null || capturedAts.isEmpty()) {
      throw invalidCapturedAt();
    }
    List<Instant> ordered = capturedAts.stream().distinct().sorted().toList();
    List<LocalDateTime> orderedUtc = ordered.stream().map(this::toUtcDateTime).toList();
    return new RequestedInstants(ordered, orderedUtc);
  }

  private LocalDateTime toUtcDateTime(Instant instant) {
    try {
      return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    } catch (DateTimeException exception) {
      throw invalidCapturedAt();
    }
  }

  private static InvalidMarketPriceQueryException invalidCapturedAt() {
    return new InvalidMarketPriceQueryException(ErrorDefinition.MARKET_PRICE_CAPTURED_AT_INVALID);
  }

  /** Keeps the requested instants and their UTC database values in matching sorted order. */
  private record RequestedInstants(List<Instant> ordered, List<LocalDateTime> orderedUtc) {}
}

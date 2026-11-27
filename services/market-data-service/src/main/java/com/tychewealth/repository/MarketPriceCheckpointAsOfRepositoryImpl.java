package com.tychewealth.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** JDBC implementation for the batched as-of checkpoint query. */
@Repository
public class MarketPriceCheckpointAsOfRepositoryImpl
    implements MarketPriceCheckpointAsOfRepository {

  private static final int MAX_REQUESTED_PAIRS_PER_QUERY = 10_000;
  private static final String VALUE_ROW = "(?, CAST(? AS TIMESTAMP), %d, %d)";

  private final JdbcTemplate jdbcTemplate;

  public MarketPriceCheckpointAsOfRepositoryImpl(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public List<MarketPriceCheckpointAtInstantProjection> findLatestBySymbolsAtInstants(
      List<String> symbols, List<LocalDateTime> capturedAts) {
    if (symbols.isEmpty() || capturedAts.isEmpty()) {
      return List.of();
    }

    int totalPairs = symbols.size() * capturedAts.size();
    List<MarketPriceCheckpointAtInstantProjection> results = new ArrayList<>(totalPairs);
    for (int start = 0; start < totalPairs; start += MAX_REQUESTED_PAIRS_PER_QUERY) {
      int end = Math.min(start + MAX_REQUESTED_PAIRS_PER_QUERY, totalPairs);
      results.addAll(queryPairs(symbols, capturedAts, start, end));
    }
    return List.copyOf(results);
  }

  private List<MarketPriceCheckpointAtInstantProjection> queryPairs(
      List<String> symbols, List<LocalDateTime> capturedAts, int start, int end) {
    StringBuilder values = new StringBuilder();
    List<Object> parameters = new ArrayList<>((end - start) * 2);
    for (int pairIndex = start; pairIndex < end; pairIndex++) {
      if (!values.isEmpty()) {
        values.append(", ");
      }
      int requestedIndex = pairIndex / symbols.size();
      int symbolIndex = pairIndex % symbols.size();
      values.append(VALUE_ROW.formatted(requestedIndex + 1, symbolIndex + 1));
      parameters.add(symbols.get(symbolIndex));
      parameters.add(Timestamp.valueOf(capturedAts.get(requestedIndex)));
    }

    String sql =
        """
        with requested(symbol, requested_at, requested_index, symbol_order) as (
          values %s
        )
        select requested.symbol as symbol,
               checkpoint.price as price,
               checkpoint.captured_at as captured_at,
               checkpoint.bucket_start as bucket_start,
               requested.requested_index as requested_index
        from requested
        join market_symbols market_symbol on market_symbol.symbol = requested.symbol
        join market_price_checkpoints checkpoint
          on checkpoint.id = (
            select candidate.id
            from market_price_checkpoints candidate
            where candidate.market_symbol_id = market_symbol.id
              and candidate.captured_at <= requested.requested_at
            order by candidate.captured_at desc, candidate.id desc
            limit 1
          )
        order by requested.requested_index, requested.symbol_order
        """
            .formatted(values);

    return jdbcTemplate.query(
        sql,
        (resultSet, rowNumber) ->
            new MarketPriceCheckpointAtInstantProjection(
                resultSet.getString("symbol"),
                resultSet.getBigDecimal("price"),
                resultSet.getTimestamp("captured_at").toLocalDateTime(),
                resultSet.getTimestamp("bucket_start").toLocalDateTime(),
                resultSet.getLong("requested_index")),
        parameters.toArray());
  }
}

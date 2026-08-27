package com.tychewealth.mapper;

import com.tychewealth.entity.MarketPriceCheckpointEntity;
import com.tychewealth.model.MarketPriceCheckpointBatch;
import com.tychewealth.model.MarketPriceCheckpointRow;
import java.time.LocalDateTime;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/** Maps checkpoint entities to the batch format required by the checkpoint repository. */
@Mapper(componentModel = "spring")
public interface MarketPriceCheckpointMapper {

  @Mapping(target = "marketSymbolId", source = "marketSymbol.id", qualifiedByName = "toStringValue")
  @Mapping(target = "price", source = "price", qualifiedByName = "toPlainString")
  @Mapping(target = "capturedAt", source = "capturedAt", qualifiedByName = "toSqlTimestamp")
  @Mapping(target = "bucketStart", source = "bucketStart", qualifiedByName = "toSqlTimestamp")
  MarketPriceCheckpointRow toRow(MarketPriceCheckpointEntity checkpoint);

  List<MarketPriceCheckpointRow> toRows(List<MarketPriceCheckpointEntity> checkpoints);

  default MarketPriceCheckpointBatch toBatch(List<MarketPriceCheckpointEntity> checkpoints) {
    List<MarketPriceCheckpointRow> rows = toRows(checkpoints);
    return new MarketPriceCheckpointBatch(
        rows.stream().map(MarketPriceCheckpointRow::marketSymbolId).toList(),
        rows.stream().map(MarketPriceCheckpointRow::price).toList(),
        rows.stream().map(MarketPriceCheckpointRow::capturedAt).toList(),
        rows.stream().map(MarketPriceCheckpointRow::bucketStart).toList());
  }

  @Named("toStringValue")
  default String toStringValue(Long value) {
    return value.toString();
  }

  @Named("toPlainString")
  default String toPlainString(java.math.BigDecimal value) {
    return value.toPlainString();
  }

  @Named("toSqlTimestamp")
  default String toSqlTimestamp(LocalDateTime timestamp) {
    return timestamp.toString().replace('T', ' ');
  }
}

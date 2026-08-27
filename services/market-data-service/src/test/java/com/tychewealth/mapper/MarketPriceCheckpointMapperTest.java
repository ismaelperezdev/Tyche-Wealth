package com.tychewealth.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.tychewealth.entity.MarketPriceCheckpointEntity;
import com.tychewealth.entity.MarketSymbolEntity;
import com.tychewealth.model.MarketPriceCheckpointBatch;
import com.tychewealth.model.MarketPriceCheckpointRow;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class MarketPriceCheckpointMapperTest {

  private static final LocalDateTime CAPTURED_AT = LocalDateTime.of(2026, 8, 27, 12, 1);
  private static final LocalDateTime BUCKET_START = LocalDateTime.of(2026, 8, 27, 12, 0);

  private final MarketPriceCheckpointMapper mapper =
      Mappers.getMapper(MarketPriceCheckpointMapper.class);

  @Test
  void shouldMapCheckpointEntityToDatabaseRow() {
    MarketPriceCheckpointEntity checkpoint = checkpoint(42L, "123.45000000");

    MarketPriceCheckpointRow result = mapper.toRow(checkpoint);

    assertThat(result)
        .isEqualTo(
            new MarketPriceCheckpointRow(
                "42", "123.45000000", "2026-08-27 12:01", "2026-08-27 12:00"));
  }

  @Test
  void shouldKeepValuesAlignedWhenMappingBatch() {
    MarketPriceCheckpointBatch result =
        mapper.toBatch(List.of(checkpoint(1L, "10.00"), checkpoint(2L, "20.00")));

    assertThat(result.marketSymbolIds()).containsExactly("1", "2");
    assertThat(result.prices()).containsExactly("10.00", "20.00");
    assertThat(result.capturedAts()).containsExactly("2026-08-27 12:01", "2026-08-27 12:01");
    assertThat(result.bucketStarts()).containsExactly("2026-08-27 12:00", "2026-08-27 12:00");
  }

  private MarketPriceCheckpointEntity checkpoint(Long symbolId, String price) {
    MarketSymbolEntity symbol = new MarketSymbolEntity();
    symbol.setId(symbolId);
    symbol.setSymbol("SYM" + symbolId);
    return new MarketPriceCheckpointEntity(
        symbol, new BigDecimal(price), CAPTURED_AT, BUCKET_START);
  }
}

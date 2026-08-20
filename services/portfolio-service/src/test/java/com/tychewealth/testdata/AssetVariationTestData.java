package com.tychewealth.testdata;

import com.tychewealth.dto.asset.AssetVariationDto;
import com.tychewealth.entity.AssetEntity;
import com.tychewealth.enums.AssetVariationTypeEnum;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class AssetVariationTestData {

  public static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 8, 19, 20, 0);

  private AssetVariationTestData() {}

  public static AssetEntity asset(Long id, String quantity, String averagePrice) {
    AssetEntity asset = new AssetEntity();
    asset.setId(id);
    asset.setQuantity(new BigDecimal(quantity));
    asset.setAveragePrice(new BigDecimal(averagePrice));
    return asset;
  }

  public static AssetVariationDto variationDto(
      Long assetId,
      AssetVariationTypeEnum changeType,
      String previousQuantity,
      String newQuantity,
      String previousAveragePrice,
      String newAveragePrice) {
    return new AssetVariationDto(
        null,
        assetId,
        changeType,
        decimal(previousQuantity),
        decimal(newQuantity),
        decimal(previousAveragePrice),
        decimal(newAveragePrice),
        OCCURRED_AT);
  }

  private static BigDecimal decimal(String value) {
    return value == null ? null : new BigDecimal(value);
  }
}

package com.tychewealth.testdata;

import com.tychewealth.dto.asset.AssetVariationDto;
import com.tychewealth.entity.AssetEntity;
import com.tychewealth.entity.AssetVariationEntity;
import com.tychewealth.enums.AssetVariationTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class AssetVariationTestData {

  public static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 8, 19, 20, 0);

  private AssetVariationTestData() {}

  public static AssetEntity asset(Long id, String quantity, String averagePrice) {
    AssetEntity asset = new AssetEntity();
    asset.setId(id);
    asset.setSymbol("TEST");
    asset.setCurrency(CurrencyCodeEnum.EUR);
    asset.setQuantity(new BigDecimal(quantity));
    asset.setAveragePrice(new BigDecimal(averagePrice));
    return asset;
  }

  public static AssetVariationEntity createdVariation(
      AssetEntity asset, String quantity, String averagePrice, LocalDateTime occurredAt) {
    return variation(
        asset, AssetVariationTypeEnum.CREATED, null, quantity, null, averagePrice, occurredAt);
  }

  public static AssetVariationEntity manualUpdateVariation(
      AssetEntity asset,
      String previousQuantity,
      String newQuantity,
      String previousAveragePrice,
      String newAveragePrice,
      LocalDateTime occurredAt) {
    return variation(
        asset,
        AssetVariationTypeEnum.MANUAL_UPDATE,
        previousQuantity,
        newQuantity,
        previousAveragePrice,
        newAveragePrice,
        occurredAt);
  }

  public static AssetVariationEntity deletedVariation(
      AssetEntity asset,
      String previousQuantity,
      String previousAveragePrice,
      LocalDateTime occurredAt) {
    return variation(
        asset,
        AssetVariationTypeEnum.DELETED,
        previousQuantity,
        null,
        previousAveragePrice,
        null,
        occurredAt);
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

  private static AssetVariationEntity variation(
      AssetEntity asset,
      AssetVariationTypeEnum changeType,
      String previousQuantity,
      String newQuantity,
      String previousAveragePrice,
      String newAveragePrice,
      LocalDateTime occurredAt) {
    return new AssetVariationEntity(
        asset,
        changeType,
        decimal(previousQuantity),
        decimal(newQuantity),
        decimal(previousAveragePrice),
        decimal(newAveragePrice),
        occurredAt);
  }
}

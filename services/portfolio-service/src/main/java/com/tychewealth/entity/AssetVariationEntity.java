package com.tychewealth.entity;

import com.tychewealth.enums.AssetVariationTypeEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/** JPA entity representing an immutable asset variation in the {@code asset_variations} table. */
@Entity
@Getter
@Immutable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "asset_variations")
public class AssetVariationEntity {

  @Id
  @SequenceGenerator(
      name = "asset_variations_seq_gen",
      sequenceName = "asset_variations_seq",
      allocationSize = 1)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "asset_variations_seq_gen")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "asset_id", nullable = false)
  private AssetEntity asset;

  @Enumerated(EnumType.STRING)
  @Column(name = "change_type", nullable = false, length = 30)
  private AssetVariationTypeEnum changeType;

  @Column(name = "previous_quantity", precision = 19, scale = 8)
  private BigDecimal previousQuantity;

  @Column(name = "new_quantity", precision = 19, scale = 8)
  private BigDecimal newQuantity;

  @Column(name = "previous_average_price", precision = 19, scale = 4)
  private BigDecimal previousAveragePrice;

  @Column(name = "new_average_price", precision = 19, scale = 4)
  private BigDecimal newAveragePrice;

  @Column(name = "occurred_at", nullable = false)
  private LocalDateTime occurredAt;

  public AssetVariationEntity(
      AssetEntity asset,
      AssetVariationTypeEnum changeType,
      BigDecimal previousQuantity,
      BigDecimal newQuantity,
      BigDecimal previousAveragePrice,
      BigDecimal newAveragePrice,
      LocalDateTime occurredAt) {
    this.asset = asset;
    this.changeType = changeType;
    this.previousQuantity = previousQuantity;
    this.newQuantity = newQuantity;
    this.previousAveragePrice = previousAveragePrice;
    this.newAveragePrice = newAveragePrice;
    this.occurredAt = occurredAt;
  }
}

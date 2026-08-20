package com.tychewealth.dto.asset;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tychewealth.enums.AssetVariationTypeEnum;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Internal data transfer object representing an asset variation. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssetVariationDto {

  private Long id;
  private Long assetId;
  private AssetVariationTypeEnum changeType;
  private BigDecimal previousQuantity;
  private BigDecimal newQuantity;
  private BigDecimal previousAveragePrice;
  private BigDecimal newAveragePrice;
  private LocalDateTime occurredAt;
}

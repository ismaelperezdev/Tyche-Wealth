package com.tychewealth.dto.asset;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssetResponseDto {

  private Long id;
  private CurrencyCodeEnum currency;
  private String symbol;
  private AssetTypeEnum assetType;
  private BigDecimal quantity;
  private BigDecimal averagePrice;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}

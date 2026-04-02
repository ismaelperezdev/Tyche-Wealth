package com.tychewealth.dto.asset.request;

import static com.tychewealth.constants.ValidationConstants.MUST_BE_AT_MOST_20_CHARACTERS;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_GREATER_THAN_0;
import static com.tychewealth.constants.ValidationConstants.MUST_HAVE_UP_TO_11_INTEGER_DIGITS_AND_8_DECIMALS;
import static com.tychewealth.constants.ValidationConstants.MUST_HAVE_UP_TO_15_INTEGER_DIGITS_AND_4_DECIMALS;
import static com.tychewealth.constants.ValidationConstants.MUST_NOT_BE_BLANK;

import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssetUpdateRequestDto {

  @Size(max = 20, message = MUST_BE_AT_MOST_20_CHARACTERS)
  @Pattern(regexp = ".*\\S.*", message = MUST_NOT_BE_BLANK)
  private String symbol;

  private AssetTypeEnum assetType;

  @Digits(integer = 11, fraction = 8, message = MUST_HAVE_UP_TO_11_INTEGER_DIGITS_AND_8_DECIMALS)
  @DecimalMin(value = "0.00000001", message = MUST_BE_GREATER_THAN_0)
  private BigDecimal quantity;

  @Digits(integer = 15, fraction = 4, message = MUST_HAVE_UP_TO_15_INTEGER_DIGITS_AND_4_DECIMALS)
  @DecimalMin(value = "0.0000", inclusive = false, message = MUST_BE_GREATER_THAN_0)
  private BigDecimal averagePrice;

  private CurrencyCodeEnum currency;
}

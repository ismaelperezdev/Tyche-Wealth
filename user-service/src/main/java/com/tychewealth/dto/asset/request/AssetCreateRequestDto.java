package com.tychewealth.dto.asset.request;

import com.tychewealth.enums.AssetTypeEnum;
import com.tychewealth.enums.CurrencyCodeEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AssetCreateRequestDto {

    @NotBlank(message = "Symbol cannot be blank")
    @Size(max = 20, message = "Symbol must be at most 20 characters")
    private String symbol;

    @NotNull(message = "Asset type cannot be null")
    private AssetTypeEnum assetType;

    @NotNull(message = "Quantity cannot be null")
    @Digits(integer = 11, fraction = 8, message = "Quantity must have up to 11 integer digits and 8 decimals")
    @DecimalMin(value = "0.00000001", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @NotNull(message = "Average price cannot be null")
    @Digits(integer = 15, fraction = 4, message = "Average price must have up to 15 integer digits and 4 decimals")
    @DecimalMin(value = "0.0000", inclusive = false, message = "Average price must be greater than 0")
    private BigDecimal averagePrice;

    @NotNull(message = "Currency cannot be null")
    private CurrencyCodeEnum currency;
}

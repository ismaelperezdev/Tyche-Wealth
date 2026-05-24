package com.tychewealth.dto.asset.request;

import static com.tychewealth.constants.ValidationConstants.MUST_NOT_BE_NULL;

import com.tychewealth.enums.AssetBatchActionEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssetBatchCreateRequestDto {

  @NotNull(message = MUST_NOT_BE_NULL)
  private AssetBatchActionEnum action;

  private String importId;

  @Valid private List<AssetCreateRequestDto> assets;
}

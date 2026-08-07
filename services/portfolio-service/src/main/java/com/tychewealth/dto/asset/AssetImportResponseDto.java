package com.tychewealth.dto.asset;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Represents the identifier and parsed candidates produced by an asset import. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssetImportResponseDto {

  private String importId;
  private List<AssetImportCandidateDto> assets;
}

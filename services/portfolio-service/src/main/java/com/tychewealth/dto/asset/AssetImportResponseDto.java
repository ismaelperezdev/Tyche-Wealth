package com.tychewealth.dto.asset;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssetImportResponseDto {

  private String fileName;
  private String extractedText;
  private String aiResponse;
  private List<AssetImportCandidateDto> assets;
}

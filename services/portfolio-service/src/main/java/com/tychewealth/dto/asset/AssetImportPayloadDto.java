package com.tychewealth.dto.asset;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssetImportPayloadDto {

  private String fileName;
  private String extractedText;
}

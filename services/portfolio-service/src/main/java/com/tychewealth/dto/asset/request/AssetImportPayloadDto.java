package com.tychewealth.dto.asset.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Carries the source file name and extracted text used during asset import processing. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssetImportPayloadDto {

  private String fileName;
  private String extractedText;
}

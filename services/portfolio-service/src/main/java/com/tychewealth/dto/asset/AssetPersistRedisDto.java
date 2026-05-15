package com.tychewealth.dto.asset;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssetPersistRedisDto {

  private String importId;
  private Long userId;
  private String fileName;
  private Instant createdAt;
  private AssetImportResponseDto result;
}

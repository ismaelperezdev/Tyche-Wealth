package com.tychewealth.dto.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiPropertiesDto(
    String baseUrl,
    String apiKey,
    String fastModel,
    String complexModel,
    long connectTimeoutSeconds,
    long requestTimeoutSeconds) {

  public AiPropertiesDto {
    baseUrl = normalizeBaseUrl(baseUrl);
    apiKey = apiKey == null ? "" : apiKey.trim();
    fastModel = fastModel == null || fastModel.isBlank() ? "deepseek-coder:6.7b" : fastModel.trim();
    complexModel =
        complexModel == null || complexModel.isBlank() ? "deepseek-r1:8b" : complexModel.trim();
    connectTimeoutSeconds = connectTimeoutSeconds <= 0 ? 10L : connectTimeoutSeconds;
    requestTimeoutSeconds = requestTimeoutSeconds <= 0 ? 60L : requestTimeoutSeconds;
  }

  public String modelFor(AiModelTypeEnum modelType) {
    return modelType == AiModelTypeEnum.COMPLEX ? complexModel : fastModel;
  }

  private static String normalizeBaseUrl(String value) {
    if (value == null || value.isBlank()) {
      return "http://localhost:11434/v1";
    }
    return value.trim();
  }
}

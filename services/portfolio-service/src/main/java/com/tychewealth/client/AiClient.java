package com.tychewealth.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tychewealth.dto.ai.AiModelTypeEnum;
import com.tychewealth.dto.ai.AiPropertiesDto;
import com.tychewealth.utils.Utils;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiClient {

  private final HttpClient aiHttpClient;
  private final ObjectMapper objectMapper;
  private final AiPropertiesDto aiProperties;

  public String prompt(String prompt, AiModelTypeEnum modelType) {
    validatePrompt(prompt);
    validateModelType(modelType);

    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder()
            .uri(URI.create(aiProperties.baseUrl().replaceAll("/+$", "") + "/chat/completions"))
            .timeout(Duration.ofSeconds(aiProperties.requestTimeoutSeconds()))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    applyAuthorizationHeader(requestBuilder);

    HttpRequest request =
        requestBuilder
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    serialize(buildRequestBody(prompt, modelType)), StandardCharsets.UTF_8))
            .build();

    HttpResponse<String> response = send(request);
    if (response.statusCode() >= 400) {
      String responseBody = response.body() == null ? "" : response.body().trim();
      throw new IllegalStateException(
          "AI request failed with HTTP "
              + response.statusCode()
              + " (responseFingerprint="
              + Utils.sha256Hex(responseBody)
              + ")");
    }

    JsonNode responseBody = deserialize(response.body());
    JsonNode choicesNode = responseBody.path("choices");
    if (!choicesNode.isArray() || choicesNode.isEmpty()) {
      throw new IllegalStateException("AI response did not include choices");
    }

    String content = choicesNode.path(0).path("message").path("content").asText("");
    if (content.isBlank()) {
      throw new IllegalStateException("AI response did not include message content");
    }

    return content.trim();
  }

  private void validatePrompt(String prompt) {
    if (prompt == null || prompt.isBlank()) {
      throw new IllegalArgumentException("prompt must not be null or empty");
    }
  }

  private void validateModelType(AiModelTypeEnum modelType) {
    if (modelType == null) {
      throw new IllegalArgumentException("modelType must not be null");
    }
  }

  private void applyAuthorizationHeader(HttpRequest.Builder requestBuilder) {
    String apiKey = aiProperties.apiKey();
    if (apiKey == null || apiKey.isBlank()) {
      return;
    }
    requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
  }

  private HttpResponse<String> send(HttpRequest request) {
    try {
      return aiHttpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to reach AI server at " + aiProperties.baseUrl(), ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("AI request was interrupted", ex);
    }
  }

  private ObjectNode buildRequestBody(String prompt, AiModelTypeEnum modelType) {
    ObjectNode root = objectMapper.createObjectNode();
    root.put("model", aiProperties.modelFor(modelType));
    root.put("stream", false);

    ArrayNode messages = root.putArray("messages");
    ObjectNode userMessage = messages.addObject();
    userMessage.put("role", "user");
    userMessage.put("content", prompt);

    return root;
  }

  private String serialize(ObjectNode requestBody) {
    try {
      return objectMapper.writeValueAsString(requestBody);
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to serialize AI request", ex);
    }
  }

  private JsonNode deserialize(String responseBody) {
    try {
      return objectMapper.readTree(responseBody);
    } catch (IOException ex) {
      throw new IllegalStateException(
          "Unable to parse AI response (responseFingerprint=" + Utils.sha256Hex(responseBody) + ")",
          ex);
    }
  }
}

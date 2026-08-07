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

/**
 * HTTP client for chat-completion providers used by portfolio asset import.
 *
 * <p>Builds and sends non-streaming chat requests using the model selected by {@link
 * AiModelTypeEnum}, applies the configured request timeout and optional bearer credential, and
 * extracts the first response message. Transport, serialization, protocol, and malformed response
 * failures are converted into explicit application exceptions with response fingerprints instead of
 * exposing full provider payloads.
 */
@Component
@RequiredArgsConstructor
public class AiClient {

  private final HttpClient aiHttpClient;
  private final ObjectMapper objectMapper;
  private final AiPropertiesDto aiProperties;

  public String prompt(String prompt, AiModelTypeEnum modelType) {

    if (prompt == null || prompt.isBlank()) {
      throw new IllegalArgumentException("prompt must not be null or empty");
    }
    if (modelType == null) {
      throw new IllegalArgumentException("modelType must not be null");
    }

    String apiKey = aiProperties.apiKey();
    ObjectNode requestBody = objectMapper.createObjectNode();
    ArrayNode messages = requestBody.putArray("messages");
    ObjectNode userMessage = messages.addObject();
    String serializedRequestBody;
    HttpResponse<String> response;

    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder()
            .uri(URI.create(aiProperties.baseUrl().replaceAll("/+$", "") + "/chat/completions"))
            .timeout(Duration.ofSeconds(aiProperties.requestTimeoutSeconds()))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

    if (apiKey != null && !apiKey.isBlank()) {
      requestBuilder.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
    }

    requestBody.put("model", aiProperties.modelFor(modelType));
    requestBody.put("stream", false);

    userMessage.put("role", "user");
    userMessage.put("content", prompt);

    try {
      serializedRequestBody = objectMapper.writeValueAsString(requestBody);
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to serialize AI request", ex);
    }

    HttpRequest request =
        requestBuilder
            .POST(
                HttpRequest.BodyPublishers.ofString(serializedRequestBody, StandardCharsets.UTF_8))
            .build();

    try {
      response =
          aiHttpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to reach AI server at " + aiProperties.baseUrl(), ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("AI request was interrupted", ex);
    }

    if (response.statusCode() >= 400) {
      String responseBody = response.body() == null ? "" : response.body().trim();
      throw new IllegalStateException(
          "AI request failed with HTTP "
              + response.statusCode()
              + " (responseFingerprint="
              + Utils.sha256Hex(responseBody)
              + ")");
    }

    JsonNode responseBody;

    try {
      responseBody = objectMapper.readTree(response.body());
    } catch (IOException ex) {
      throw new IllegalStateException(
          "Unable to parse AI response (responseFingerprint="
              + Utils.sha256Hex(response.body())
              + ")",
          ex);
    }

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
}

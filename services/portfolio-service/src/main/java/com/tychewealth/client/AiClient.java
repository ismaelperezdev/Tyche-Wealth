package com.tychewealth.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tychewealth.dto.ai.AiModelTypeEnum;
import com.tychewealth.dto.ai.AiPropertiesDto;
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
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(aiProperties.baseUrl().replaceAll("/+$", "") + "/chat/completions"))
            .timeout(Duration.ofSeconds(aiProperties.timeoutSeconds()))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .headers(buildAuthorizationHeaders())
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    serialize(buildRequestBody(prompt, modelType)), StandardCharsets.UTF_8))
            .build();

    HttpResponse<String> response = send(request);
    if (response.statusCode() >= 400) {
      throw new IllegalStateException(
          "AI request failed with HTTP " + response.statusCode() + ": " + response.body());
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

  private String[] buildAuthorizationHeaders() {
    if (aiProperties.apiKey().isBlank()) {
      return new String[0];
    }
    return new String[] {HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.apiKey()};
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
      throw new IllegalStateException("Unable to parse AI response: " + responseBody, ex);
    }
  }
}

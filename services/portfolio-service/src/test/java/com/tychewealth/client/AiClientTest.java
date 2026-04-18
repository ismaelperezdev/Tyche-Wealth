package com.tychewealth.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.dto.ai.AiModelTypeEnum;
import com.tychewealth.dto.ai.AiPropertiesDto;
import com.tychewealth.utils.Utils;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

class AiClientTest {

  @Test
  void promptOmitsResponseBodyFromHttpErrorMessage() throws Exception {
    HttpClient httpClient = mock(HttpClient.class);
    @SuppressWarnings("unchecked")
    HttpResponse<String> response = mock(HttpResponse.class);
    AiClient aiClient =
        new AiClient(
            httpClient,
            new ObjectMapper(),
            new AiPropertiesDto(
                "http://localhost:11434/v1", "", "fast-model", "complex-model", 10, 30));
    String sensitiveBody = "{\"error\":\"secret-token-123\",\"details\":\"private-content\"}";

    when(response.statusCode()).thenReturn(500);
    when(response.body()).thenReturn(sensitiveBody);
    when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> aiClient.prompt("extract assets", AiModelTypeEnum.FAST));

    String message = exception.getMessage();
    assertTrue(message.contains("AI request failed with HTTP 500"));
    assertTrue(
        message.contains("responseFingerprint=" + Utils.sha256Hex(sensitiveBody)),
        "Expected response fingerprint in the error message");
    assertFalse(message.contains("secret-token-123"));
    assertFalse(message.contains("private-content"));
    assertFalse(message.contains("body="));
  }
}

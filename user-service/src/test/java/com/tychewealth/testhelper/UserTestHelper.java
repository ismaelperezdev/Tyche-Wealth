package com.tychewealth.testhelper;

import static com.tychewealth.constants.ApiConstants.USER_ME_PASSWORD_URL;
import static com.tychewealth.constants.ApiConstants.USER_ME_URL;
import static com.tychewealth.constants.AuthConstants.AUTHORIZATION_HEADER;
import static com.tychewealth.constants.AuthConstants.TOKEN_TYPE_BEARER_PREFIX;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

public final class UserTestHelper {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private UserTestHelper() {}

  public static String authorizationHeader(String accessToken) {
    return TOKEN_TYPE_BEARER_PREFIX + accessToken;
  }

  public static ResultActions retrieveRequest(MockMvc mockMvc, String accessToken)
      throws Exception {
    return mockMvc.perform(
        get(USER_ME_URL).header(AUTHORIZATION_HEADER, authorizationHeader(accessToken)));
  }

  public static ResultActions retrieveRequestUnauthorized(MockMvc mockMvc) throws Exception {
    return mockMvc.perform(get(USER_ME_URL));
  }

  public static ResultActions updateRequest(
      MockMvc mockMvc, ObjectMapper objectMapper, String accessToken, String username)
      throws Exception {
    return mockMvc.perform(
        patch(USER_ME_URL)
            .header(AUTHORIZATION_HEADER, authorizationHeader(accessToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("username", username))));
  }

  public static ResultActions updateRequestUnauthorized(
      MockMvc mockMvc, ObjectMapper objectMapper, String username) throws Exception {
    return mockMvc.perform(
        patch(USER_ME_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("username", username))));
  }

  public static ResultActions updatePasswordRequest(
      MockMvc mockMvc,
      String accessToken,
      String currentPassword,
      String newPassword,
      String confirmNewPassword)
      throws Exception {
    return mockMvc.perform(
        patch(USER_ME_PASSWORD_URL)
            .header(AUTHORIZATION_HEADER, authorizationHeader(accessToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(passwordUpdateRequestBody(currentPassword, newPassword, confirmNewPassword)));
  }

  public static ResultActions updatePasswordRequestUnauthorized(
      MockMvc mockMvc, String currentPassword, String newPassword, String confirmNewPassword)
      throws Exception {
    return mockMvc.perform(
        patch(USER_ME_PASSWORD_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(passwordUpdateRequestBody(currentPassword, newPassword, confirmNewPassword)));
  }

  public static ResultActions deleteRequest(MockMvc mockMvc, String accessToken) throws Exception {
    return mockMvc.perform(
        delete(USER_ME_URL).header(AUTHORIZATION_HEADER, authorizationHeader(accessToken)));
  }

  public static ResultActions deleteRequestUnauthorized(MockMvc mockMvc) throws Exception {
    return mockMvc.perform(delete(USER_ME_URL));
  }

  public static String passwordUpdateRequestBody(
      String currentPassword, String newPassword, String confirmNewPassword) {
    try {
      return OBJECT_MAPPER.writeValueAsString(
          Map.of(
              "currentPassword", currentPassword,
              "newPassword", newPassword,
              "confirmNewPassword", confirmNewPassword));
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to serialize password update request body", ex);
    }
  }

  public static String updateRequestBody(String username) {
    try {
      return OBJECT_MAPPER.writeValueAsString(Map.of("username", username));
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to serialize update request body", ex);
    }
  }

  public static String buildOverBcryptLimitPassword() {
    return "Aa1!" + "\u00E1".repeat(35);
  }
}

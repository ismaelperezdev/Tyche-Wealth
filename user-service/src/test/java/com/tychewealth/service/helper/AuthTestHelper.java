package com.tychewealth.service.helper;

import static com.tychewealth.constants.ApiConstants.AUTH_LOGIN_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_LOGOUT_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_REFRESH_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_REGISTER_URL;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.dto.auth.LoginResponseDto;
import com.tychewealth.dto.auth.request.LoginRequestDto;
import com.tychewealth.dto.auth.request.RefreshTokenRequestDto;
import com.tychewealth.dto.auth.request.RegisterRequestDto;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

public final class AuthTestHelper {

  private AuthTestHelper() {}

  public static LoginResponseDto login(
      MockMvc mockMvc, ObjectMapper objectMapper, LoginRequestDto request) throws Exception {
    String responseBody =
        loginRequest(mockMvc, objectMapper, request)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    return objectMapper.readValue(responseBody, LoginResponseDto.class);
  }

  public static ResultActions registerRequest(
      MockMvc mockMvc, ObjectMapper objectMapper, RegisterRequestDto request) throws Exception {
    return mockMvc.perform(
        post(AUTH_REGISTER_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));
  }

  public static ResultActions loginRequest(
      MockMvc mockMvc, ObjectMapper objectMapper, LoginRequestDto request) throws Exception {
    return mockMvc.perform(
        post(AUTH_LOGIN_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));
  }

  public static ResultActions refresh(
      MockMvc mockMvc, ObjectMapper objectMapper, String refreshToken) throws Exception {
    return mockMvc.perform(
        post(AUTH_REFRESH_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new RefreshTokenRequestDto(refreshToken))));
  }

  public static ResultActions logout(
      MockMvc mockMvc, ObjectMapper objectMapper, String refreshToken) throws Exception {
    return mockMvc.perform(
        post(AUTH_LOGOUT_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new RefreshTokenRequestDto(refreshToken))));
  }
}

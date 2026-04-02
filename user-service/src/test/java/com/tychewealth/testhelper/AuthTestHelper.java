package com.tychewealth.testhelper;

import static com.tychewealth.constants.ApiConstants.AUTH_BASE_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_LOGIN_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_LOGOUT_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_REFRESH_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_REGISTER_URL;
import static com.tychewealth.constants.AuthConstants.*;
import static com.tychewealth.constants.TestConstants.TEST_TRUSTED_DEVICE_COOKIE_NAME;
import static com.tychewealth.constants.TestConstants.TEST_VERIFY_REGISTRATION_PATH;
import static com.tychewealth.testdata.EntityBuilder.buildTrustedDevice;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.dto.auth.LoginResponseDto;
import com.tychewealth.dto.auth.request.ForgotPasswordRequestDto;
import com.tychewealth.dto.auth.request.LoginRequestDto;
import com.tychewealth.dto.auth.request.RefreshTokenRequestDto;
import com.tychewealth.dto.auth.request.RegisterRequestDto;
import com.tychewealth.dto.auth.request.ResendVerificationEmailRequestDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.repository.TrustedDeviceRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

public final class AuthTestHelper {

  private static final String TEST_AUTH_TOKEN_PARAM = "token";
  private static final String TEST_VERIFY_LOGIN_DEVICE_PATH = "/verify-login-device";

  private AuthTestHelper() {}

  public static LoginResponseDto login(
      MockMvc mockMvc, ObjectMapper objectMapper, LoginRequestDto request) throws Exception {
    return login(mockMvc, objectMapper, request, null);
  }

  public static LoginResponseDto login(
      MockMvc mockMvc, ObjectMapper objectMapper, LoginRequestDto request, Cookie trustedDevice)
      throws Exception {
    String responseBody =
        loginRequest(mockMvc, objectMapper, request, trustedDevice)
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
    return loginRequest(mockMvc, objectMapper, request, null);
  }

  public static ResultActions loginRequest(
      MockMvc mockMvc, ObjectMapper objectMapper, LoginRequestDto request, Cookie trustedDevice)
      throws Exception {
    var requestBuilder =
        post(AUTH_LOGIN_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request));

    if (trustedDevice != null) {
      requestBuilder.cookie(trustedDevice);
    }

    return mockMvc.perform(requestBuilder);
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
    return logout(mockMvc, objectMapper, null, refreshToken);
  }

  public static ResultActions logout(
      MockMvc mockMvc, ObjectMapper objectMapper, String accessToken, String refreshToken)
      throws Exception {
    var requestBuilder =
        post(AUTH_LOGOUT_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new RefreshTokenRequestDto(refreshToken)));

    if (accessToken != null && !accessToken.isBlank()) {
      requestBuilder.header(AUTHORIZATION_HEADER, TOKEN_TYPE_BEARER_PREFIX + accessToken);
    }

    return mockMvc.perform(requestBuilder);
  }

  public static ResultActions verifyRegistrationRequest(MockMvc mockMvc, String token)
      throws Exception {
    return verifyRegistrationRequest(mockMvc, token, null);
  }

  public static ResultActions verifyRegistrationRequest(
      MockMvc mockMvc, String token, Cookie trustedDevice) throws Exception {
    var requestBuilder =
        get(AUTH_BASE_URL + TEST_VERIFY_REGISTRATION_PATH).param(TEST_AUTH_TOKEN_PARAM, token);

    if (trustedDevice != null) {
      requestBuilder.cookie(trustedDevice);
    }

    return mockMvc.perform(requestBuilder);
  }

  public static ResultActions verifyLoginDeviceRequest(MockMvc mockMvc, String token)
      throws Exception {
    return verifyLoginDeviceRequest(mockMvc, token, null);
  }

  public static ResultActions verifyLoginDeviceRequest(
      MockMvc mockMvc, String token, Cookie trustedDevice) throws Exception {
    var requestBuilder =
        get(AUTH_BASE_URL + TEST_VERIFY_LOGIN_DEVICE_PATH).param(TEST_AUTH_TOKEN_PARAM, token);

    if (trustedDevice != null) {
      requestBuilder.cookie(trustedDevice);
    }

    return mockMvc.perform(requestBuilder);
  }

  public static int forgotPasswordStatus(
      MockMvc mockMvc, ObjectMapper objectMapper, ForgotPasswordRequestDto requestDto)
      throws Exception {
    return mockMvc
        .perform(
            get(AUTH_BASE_URL + "/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  public static int resendVerificationStatus(
      MockMvc mockMvc, ObjectMapper objectMapper, ResendVerificationEmailRequestDto requestDto)
      throws Exception {
    return mockMvc
        .perform(
            post(AUTH_BASE_URL + "/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  public static int verifyRegistrationStatus(MockMvc mockMvc, String token, Cookie trustedDevice)
      throws Exception {
    return verifyRegistrationRequest(mockMvc, token, trustedDevice)
        .andReturn()
        .getResponse()
        .getStatus();
  }

  public static int verifyLoginDeviceStatus(MockMvc mockMvc, String token, Cookie trustedDevice)
      throws Exception {
    return verifyLoginDeviceRequest(mockMvc, token, trustedDevice)
        .andReturn()
        .getResponse()
        .getStatus();
  }

  public static String buildLongEmail() {
    String local = "a".repeat(64);
    String label63 = "b".repeat(63);
    String domain = label63 + "." + label63 + "." + label63 + ".es";
    return local + "@" + domain;
  }

  public static Cookie seedTrustedDevice(
      TrustedDeviceRepository trustedDeviceRepository, UserEntity user) {
    String trustedDeviceToken = "trusted-device-" + UUID.randomUUID();
    Instant now = Instant.now();
    var trustedDevice =
        buildTrustedDevice(trustedDeviceToken, user, now.plusSeconds(Integer.MAX_VALUE), now);
    trustedDeviceRepository.save(trustedDevice);

    return new Cookie(TEST_TRUSTED_DEVICE_COOKIE_NAME, trustedDeviceToken);
  }

  public static String extractCookieValue(String setCookieHeader, String cookieName) {
    Pattern cookiePattern = Pattern.compile(cookieName + "=([^;]+)");
    Matcher matcher = cookiePattern.matcher(setCookieHeader);
    org.junit.jupiter.api.Assertions.assertTrue(
        matcher.find(),
        "Expected cookie '"
            + cookieName
            + "' in Set-Cookie header but was missing. Header: "
            + setCookieHeader);
    return matcher.group(1);
  }
}

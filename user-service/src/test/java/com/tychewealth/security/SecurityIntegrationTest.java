package com.tychewealth.security;

import static com.tychewealth.constants.ApiConstants.AUTH_LOGIN_URL;
import static com.tychewealth.constants.ApiConstants.USER_ME_PASSWORD_URL;
import static com.tychewealth.constants.ApiConstants.USER_ME_URL;
import static com.tychewealth.constants.AuthConstants.AUTHORIZATION_HEADER;
import static com.tychewealth.constants.AuthConstants.TOKEN_TYPE_BEARER_PREFIX;
import static com.tychewealth.constants.SecurityConstants.ACTUATOR_PROMETHEUS_PATH;
import static com.tychewealth.constants.SecurityConstants.CACHE_CONTROL_NO_STORE_HEADER_VALUE;
import static com.tychewealth.constants.SecurityConstants.HEADER_VALUE_DENY;
import static com.tychewealth.constants.SecurityConstants.HEADER_VALUE_NOSNIFF;
import static com.tychewealth.constants.SecurityConstants.HEADER_VALUE_NO_REFERRER;
import static com.tychewealth.constants.SecurityConstants.HSTS_MAX_AGE_SECONDS;
import static com.tychewealth.constants.SecurityConstants.PRAGMA_NO_CACHE_HEADER_VALUE;
import static com.tychewealth.constants.TestConstants.TEST_ATTACKER_BASIC_TOKEN;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_HEADER_REFERRER_POLICY;
import static com.tychewealth.constants.TestConstants.TEST_HEADER_STRICT_TRANSPORT_SECURITY;
import static com.tychewealth.constants.TestConstants.TEST_HEADER_X_CONTENT_TYPE_OPTIONS;
import static com.tychewealth.constants.TestConstants.TEST_HEADER_X_FRAME_OPTIONS;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_NEW_VALID;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_VALID;
import static com.tychewealth.constants.TestConstants.TEST_TAMPERED_TOKEN_SUFFIX;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_LAURA;
import static com.tychewealth.testhelper.AuthTestHelper.login;
import static com.tychewealth.testhelper.AuthTestHelper.refresh;
import static com.tychewealth.testhelper.UserTestHelper.passwordUpdateRequestBody;
import static com.tychewealth.utils.Utils.sha256Hex;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.config.RefreshRateLimitConfig;
import com.tychewealth.config.SecurityIntegrationTestConfig;
import com.tychewealth.dto.auth.LoginResponseDto;
import com.tychewealth.dto.auth.RefreshTokenResponseDto;
import com.tychewealth.dto.auth.request.LoginRequestDto;
import com.tychewealth.entity.RefreshTokenEntity;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.RefreshTokenRepository;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.helper.token.AccessTokenHelper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(classes = SecurityIntegrationTestConfig.class)
@ContextConfiguration(initializers = SecurityIntegrationTestConfig.Initializer.class)
@AutoConfigureMockMvc
class SecurityIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private AccessTokenHelper accessTokenHelper;
  @Autowired private RefreshRateLimitConfig rateLimitConfig;

  private LoginRequestDto validLoginRequest;

  @BeforeEach
  void setUp() {
    refreshTokenRepository.deleteAll();
    userRepository.deleteAll();
    rateLimitConfig.resetAll();

    UserEntity existingUser = new UserEntity();
    existingUser.setEmail(TEST_EMAIL_LAURA);
    existingUser.setUsername(TEST_USERNAME_LAURA);
    existingUser.setPassword(passwordEncoder.encode(TEST_PASSWORD_VALID));
    userRepository.save(existingUser);

    validLoginRequest = new LoginRequestDto(TEST_EMAIL_LAURA, TEST_PASSWORD_VALID);
  }

  @Test
  void loginAddsAntiCacheAndSecurityHeaders() throws Exception {
    mockMvc
        .perform(
            post(AUTH_LOGIN_URL)
                .secure(true)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(HttpHeaders.PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(header().string(TEST_HEADER_X_CONTENT_TYPE_OPTIONS, HEADER_VALUE_NOSNIFF))
        .andExpect(header().string(TEST_HEADER_X_FRAME_OPTIONS, HEADER_VALUE_DENY))
        .andExpect(header().string(TEST_HEADER_REFERRER_POLICY, HEADER_VALUE_NO_REFERRER))
        .andExpect(
            header()
                .string(
                    TEST_HEADER_STRICT_TRANSPORT_SECURITY,
                    containsString("max-age=" + HSTS_MAX_AGE_SECONDS)))
        .andExpect(
            header()
                .string(
                    TEST_HEADER_STRICT_TRANSPORT_SECURITY, containsString("includeSubDomains")));
  }

  @Test
  void refreshTokenIsStoredHashedAfterLogin() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post(AUTH_LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validLoginRequest)))
            .andExpect(status().isOk())
            .andReturn();

    LoginResponseDto response =
        objectMapper.readValue(result.getResponse().getContentAsString(), LoginResponseDto.class);
    List<RefreshTokenEntity> storedTokens = refreshTokenRepository.findAll();

    assertEquals(1, storedTokens.size());
    assertNotNull(response.getRefreshToken());
    assertNotEquals(response.getRefreshToken(), storedTokens.getFirst().getToken());
    assertEquals(sha256Hex(response.getRefreshToken()), storedTokens.getFirst().getToken());
  }

  @Test
  void unauthorizedProtectedRequestReturnsNoStoreAndSecurityHeaders() throws Exception {
    mockMvc
        .perform(get(USER_ME_URL).secure(true))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.UNAUTHORIZED.getCode()))
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(HttpHeaders.PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(header().string(TEST_HEADER_X_CONTENT_TYPE_OPTIONS, HEADER_VALUE_NOSNIFF))
        .andExpect(header().string(TEST_HEADER_X_FRAME_OPTIONS, HEADER_VALUE_DENY))
        .andExpect(header().string(TEST_HEADER_REFERRER_POLICY, HEADER_VALUE_NO_REFERRER))
        .andExpect(
            header()
                .string(
                    TEST_HEADER_STRICT_TRANSPORT_SECURITY,
                    containsString("max-age=" + HSTS_MAX_AGE_SECONDS)));
  }

  @Test
  void invalidAuthorizationSchemeIsRejectedOnProtectedEndpoint() throws Exception {
    mockMvc
        .perform(get(USER_ME_URL).header(AUTHORIZATION_HEADER, TEST_ATTACKER_BASIC_TOKEN))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.description").value(ErrorDefinition.UNAUTHORIZED.getDescription()));
  }

  @Test
  void tamperedJwtIsRejectedOnProtectedEndpoint() throws Exception {
    UserEntity savedUser =
        userRepository.findByEmailIncludingDeleted(TEST_EMAIL_LAURA).orElseThrow();
    String accessToken = accessTokenHelper.generateAccessToken(savedUser).accessToken();
    String tamperedToken = accessToken + TEST_TAMPERED_TOKEN_SUFFIX;

    mockMvc
        .perform(
            get(USER_ME_URL).header(AUTHORIZATION_HEADER, TOKEN_TYPE_BEARER_PREFIX + tamperedToken))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.description").value(ErrorDefinition.UNAUTHORIZED.getDescription()));
  }

  @Test
  void rotatedRefreshTokenCannotBeReused() throws Exception {
    LoginResponseDto loginResponse = login(mockMvc, objectMapper, validLoginRequest);

    String rotatedBody =
        refresh(mockMvc, objectMapper, loginResponse.getRefreshToken())
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    RefreshTokenResponseDto rotatedResponse =
        objectMapper.readValue(rotatedBody, RefreshTokenResponseDto.class);

    refresh(mockMvc, objectMapper, loginResponse.getRefreshToken())
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID.getCode()));

    rateLimitConfig.resetAll();

    refresh(mockMvc, objectMapper, rotatedResponse.getRefreshToken()).andExpect(status().isOk());
  }

  @Test
  void prometheusEndpointRejectsAnonymousAccess() throws Exception {
    mockMvc
        .perform(get(ACTUATOR_PROMETHEUS_PATH).secure(true))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void updatePasswordAddsAntiCacheHeaders() throws Exception {
    UserEntity savedUser =
        userRepository.findByEmailIncludingDeleted(TEST_EMAIL_LAURA).orElseThrow();
    String accessToken = accessTokenHelper.generateAccessToken(savedUser).accessToken();

    mockMvc
        .perform(
            patch(USER_ME_PASSWORD_URL)
                .secure(true)
                .header(AUTHORIZATION_HEADER, TOKEN_TYPE_BEARER_PREFIX + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    passwordUpdateRequestBody(
                        TEST_PASSWORD_VALID, TEST_PASSWORD_NEW_VALID, TEST_PASSWORD_NEW_VALID)))
        .andExpect(status().isNoContent())
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE))
        .andExpect(header().string(HttpHeaders.PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE))
        .andExpect(header().string(TEST_HEADER_X_CONTENT_TYPE_OPTIONS, HEADER_VALUE_NOSNIFF))
        .andExpect(header().string(TEST_HEADER_X_FRAME_OPTIONS, HEADER_VALUE_DENY))
        .andExpect(header().string(TEST_HEADER_REFERRER_POLICY, HEADER_VALUE_NO_REFERRER));
  }
}

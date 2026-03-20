package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.AUTH_LOGOUT_URL;
import static com.tychewealth.constants.ApiConstants.USER_ME_PASSWORD_URL;
import static com.tychewealth.constants.ApiConstants.USER_ME_URL;
import static com.tychewealth.constants.AuthConstants.AUTHORIZATION_HEADER;
import static com.tychewealth.constants.AuthConstants.TOKEN_TYPE_BEARER;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_FIELD_CONFIRM_NEW_PASSWORD;
import static com.tychewealth.constants.TestConstants.TEST_FIELD_CURRENT_PASSWORD;
import static com.tychewealth.constants.TestConstants.TEST_FIELD_NEW_PASSWORD;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_NEW_VALID;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_VALID;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_LAURA;
import static com.tychewealth.testhelper.AuthTestHelper.login;
import static com.tychewealth.testhelper.AuthTestHelper.logout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.config.RedisIntegrationTestConfig;
import com.tychewealth.dto.auth.LoginResponseDto;
import com.tychewealth.dto.auth.request.LoginRequestDto;
import com.tychewealth.dto.auth.request.RefreshTokenRequestDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.RefreshTokenRepository;
import com.tychewealth.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = RedisIntegrationTestConfig.class)
@ContextConfiguration(initializers = RedisIntegrationTestConfig.Initializer.class)
@AutoConfigureMockMvc
class RedisIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private LoginRequestDto validLoginRequest;

  @BeforeEach
  void setUp() {
    refreshTokenRepository.deleteAll();
    userRepository.deleteAll();

    UserEntity existingLoginUser = new UserEntity();
    existingLoginUser.setEmail(TEST_EMAIL_LAURA);
    existingLoginUser.setUsername(TEST_USERNAME_LAURA);
    existingLoginUser.setPassword(passwordEncoder.encode(TEST_PASSWORD_VALID));
    userRepository.save(existingLoginUser);

    validLoginRequest = new LoginRequestDto(TEST_EMAIL_LAURA, TEST_PASSWORD_VALID);
  }

  @Test
  void logoutRevokesAccessTokenForProtectedRetrieveEndpoint() throws Exception {
    LoginResponseDto loginResponse = login(mockMvc, objectMapper, validLoginRequest);

    logout(mockMvc, objectMapper, loginResponse.getAccessToken(), loginResponse.getRefreshToken())
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get(USER_ME_URL)
                .header(
                    AUTHORIZATION_HEADER, TOKEN_TYPE_BEARER + " " + loginResponse.getAccessToken()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.UNAUTHORIZED.getType()))
        .andExpect(jsonPath("$.description").value(ErrorDefinition.UNAUTHORIZED.getDescription()));
  }

  @Test
  void logoutRevokedAccessTokenIsRejectedByProtectedWriteEndpoint() throws Exception {
    LoginResponseDto loginResponse = login(mockMvc, objectMapper, validLoginRequest);

    logout(mockMvc, objectMapper, loginResponse.getAccessToken(), loginResponse.getRefreshToken())
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            patch(USER_ME_PASSWORD_URL)
                .header(
                    AUTHORIZATION_HEADER, TOKEN_TYPE_BEARER + " " + loginResponse.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            TEST_FIELD_CURRENT_PASSWORD, TEST_PASSWORD_VALID,
                            TEST_FIELD_NEW_PASSWORD, TEST_PASSWORD_NEW_VALID,
                            TEST_FIELD_CONFIRM_NEW_PASSWORD, TEST_PASSWORD_NEW_VALID))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.UNAUTHORIZED.getType()))
        .andExpect(jsonPath("$.description").value(ErrorDefinition.UNAUTHORIZED.getDescription()));
  }

  @Test
  void logoutWithoutAuthorizationHeaderStillRevokesRefreshTokenAndAccessToken() throws Exception {
    LoginResponseDto loginResponse = login(mockMvc, objectMapper, validLoginRequest);

    logout(mockMvc, objectMapper, loginResponse.getRefreshToken())
        .andExpect(status().isNoContent());

    assertTrue(
        refreshTokenRepository
            .findByToken(loginResponse.getRefreshToken())
            .orElseThrow()
            .isRevoked());

    mockMvc
        .perform(
            get(USER_ME_URL)
                .header(
                    AUTHORIZATION_HEADER, TOKEN_TYPE_BEARER + " " + loginResponse.getAccessToken()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.UNAUTHORIZED.getType()))
        .andExpect(jsonPath("$.description").value(ErrorDefinition.UNAUTHORIZED.getDescription()));
  }

  @Test
  void logoutRejectsInvalidAuthorizationHeaderEvenWhenRefreshTokenIsValid() throws Exception {
    LoginResponseDto loginResponse = login(mockMvc, objectMapper, validLoginRequest);

    mockMvc
        .perform(
            post(AUTH_LOGOUT_URL)
                .header(AUTHORIZATION_HEADER, "Basic invalid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new RefreshTokenRequestDto(loginResponse.getRefreshToken()))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.UNAUTHORIZED.getType()))
        .andExpect(jsonPath("$.description").value(ErrorDefinition.UNAUTHORIZED.getDescription()));
  }

  @Test
  void updatePasswordRevokesCurrentAccessToken() throws Exception {
    LoginResponseDto loginResponse = login(mockMvc, objectMapper, validLoginRequest);

    mockMvc
        .perform(
            patch(USER_ME_PASSWORD_URL)
                .header(
                    AUTHORIZATION_HEADER, TOKEN_TYPE_BEARER + " " + loginResponse.getAccessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            TEST_FIELD_CURRENT_PASSWORD, TEST_PASSWORD_VALID,
                            TEST_FIELD_NEW_PASSWORD, TEST_PASSWORD_NEW_VALID,
                            TEST_FIELD_CONFIRM_NEW_PASSWORD, TEST_PASSWORD_NEW_VALID))))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get(USER_ME_URL)
                .header(
                    AUTHORIZATION_HEADER, TOKEN_TYPE_BEARER + " " + loginResponse.getAccessToken()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.UNAUTHORIZED.getType()))
        .andExpect(jsonPath("$.description").value(ErrorDefinition.UNAUTHORIZED.getDescription()));
  }

  @Test
  void deleteRevokesCurrentAccessToken() throws Exception {
    LoginResponseDto loginResponse = login(mockMvc, objectMapper, validLoginRequest);

    mockMvc
        .perform(
            delete(USER_ME_URL)
                .header(
                    AUTHORIZATION_HEADER, TOKEN_TYPE_BEARER + " " + loginResponse.getAccessToken()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            get(USER_ME_URL)
                .header(
                    AUTHORIZATION_HEADER, TOKEN_TYPE_BEARER + " " + loginResponse.getAccessToken()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.UNAUTHORIZED.getType()))
        .andExpect(jsonPath("$.description").value(ErrorDefinition.UNAUTHORIZED.getDescription()));
  }
}

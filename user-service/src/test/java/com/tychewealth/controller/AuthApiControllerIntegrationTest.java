package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.URL_FOLDER_V1;
import static com.tychewealth.testdata.EntityBuilder.buildRefreshToken;
import static com.tychewealth.testdata.EntityBuilder.buildUser;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.config.AuthIntegrationTestConfig;
import com.tychewealth.dto.user.LoginResponseDto;
import com.tychewealth.dto.user.RefreshTokenResponseDto;
import com.tychewealth.dto.user.request.LoginRequestDto;
import com.tychewealth.dto.user.request.RefreshTokenRequestDto;
import com.tychewealth.dto.user.request.RegisterRequestDto;
import com.tychewealth.entity.RefreshTokenEntity;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.repository.RefreshTokenRepository;
import com.tychewealth.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    classes = AuthIntegrationTestConfig.class,
    properties = "app.auth.jwt.secret=4AYI7d6GOEvFEcCJZkDA0hGFqI6SuF5RAsxAjqzTmaM=")
@AutoConfigureMockMvc
class AuthApiControllerIntegrationTest {

  private static final String REGISTER_URL = URL_FOLDER_V1 + "/auth/register";
  private static final String LOGIN_URL = URL_FOLDER_V1 + "/auth/login";
  private static final String REFRESH_URL = URL_FOLDER_V1 + "/auth/refresh";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private UserRepository userRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  private RegisterRequestDto validRequest;
  private LoginRequestDto validLoginRequest;
  private RegisterRequestDto conflictByEmailRequest;
  private RegisterRequestDto conflictByUsernameRequest;
  private UserEntity existingEmailUser;
  private UserEntity existingUsernameUser;
  private UserEntity existingLoginUser;

  @BeforeEach
  void setUp() {
    refreshTokenRepository.deleteAll();
    userRepository.deleteAll();
    validRequest =
        new RegisterRequestDto("laura.gomez@tychewealth.com", "lauragomez", "Secret123!");
    conflictByEmailRequest =
        new RegisterRequestDto(validRequest.getEmail(), "carlosmartin", "Secret123!");
    conflictByUsernameRequest =
        new RegisterRequestDto(
            "pablo.ortega@tychewealth.com", validRequest.getUsername(), "Secret123!");

    existingEmailUser = new UserEntity();
    existingEmailUser.setEmail(validRequest.getEmail());
    existingEmailUser.setUsername("anabelruiz");
    existingEmailUser.setPassword(passwordEncoder.encode("Secret123!"));

    existingUsernameUser = new UserEntity();
    existingUsernameUser.setEmail("mario.santos@tychewealth.com");
    existingUsernameUser.setUsername(validRequest.getUsername());
    existingUsernameUser.setPassword(passwordEncoder.encode("Secret123!"));

    existingLoginUser = new UserEntity();
    existingLoginUser.setEmail(validRequest.getEmail());
    existingLoginUser.setUsername(validRequest.getUsername());
    existingLoginUser.setPassword(passwordEncoder.encode(validRequest.getPassword()));

    validLoginRequest = new LoginRequestDto(validRequest.getEmail(), validRequest.getPassword());
  }

  @Test
  void registerCreatesUserWhenRequestIsValid() throws Exception {
    mockMvc
        .perform(
            post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.email").value(validRequest.getEmail()))
        .andExpect(jsonPath("$.username").value(validRequest.getUsername()))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.password").doesNotExist());

    UserEntity created = userRepository.findByEmail(validRequest.getEmail()).orElseThrow();
    assertNotNull(created.getId());
    assertNotEquals(validRequest.getPassword(), created.getPassword());
    assertTrue(passwordEncoder.matches(validRequest.getPassword(), created.getPassword()));
  }

  @Test
  void registerReturnsConflictWhenEmailAlreadyExists() throws Exception {
    userRepository.save(existingEmailUser);

    mockMvc
        .perform(
            post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(conflictByEmailRequest)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TYCHE-100"))
        .andExpect(jsonPath("$.type").value("AUTH_REGISTRATION_CONFLICT"))
        .andExpect(
            jsonPath("$.description").value("A user with the provided credentials already exists"));
  }

  @Test
  void registerReturnsConflictWhenUsernameAlreadyExists() throws Exception {
    userRepository.save(existingUsernameUser);

    mockMvc
        .perform(
            post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(conflictByUsernameRequest)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("TYCHE-100"))
        .andExpect(jsonPath("$.type").value("AUTH_REGISTRATION_CONFLICT"))
        .andExpect(
            jsonPath("$.description").value("A user with the provided credentials already exists"));
  }

  @ParameterizedTest
  @MethodSource("com.tychewealth.testdata.AuthTestData#invalidCreateRequests")
  void registerReturnsBadRequestForInvalidPayload(
      RegisterRequestDto invalidRequest, String expectedMessage) throws Exception {
    mockMvc
        .perform(
            post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("TYCHE-002"))
        .andExpect(jsonPath("$.type").value("GENERIC_VALIDATION_ERROR"))
        .andExpect(jsonPath("$.description").value(containsString(expectedMessage)));
  }

  @Test
  void loginReturnsTokenAndUserWhenCredentialsAreValid() throws Exception {
    userRepository.save(existingLoginUser);

    mockMvc
        .perform(
            post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.accessToken").isString())
        .andExpect(jsonPath("$.refreshToken").isString())
        .andExpect(jsonPath("$.expiresIn").isNumber())
        .andExpect(jsonPath("$.user.id").isNumber())
        .andExpect(jsonPath("$.user.email").value(validRequest.getEmail()))
        .andExpect(jsonPath("$.user.username").value(validRequest.getUsername()))
        .andExpect(jsonPath("$.user.password").doesNotExist());
  }

  @Test
  void loginReturnsUnauthorizedWhenCredentialsAreInvalid() throws Exception {
    userRepository.save(existingLoginUser);

    LoginRequestDto invalidLoginRequest = new LoginRequestDto(validRequest.getEmail(), "Wrong123!");

    mockMvc
        .perform(
            post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidLoginRequest)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("TYCHE-101"))
        .andExpect(jsonPath("$.type").value("AUTH_LOGIN_INVALID_CREDENTIALS"))
        .andExpect(jsonPath("$.description").value("The provided login credentials are invalid"));
  }

  @Test
  void secondLoginRevokesPreviousRefreshToken() throws Exception {
    userRepository.save(existingLoginUser);

    String firstLoginBody =
        mockMvc
            .perform(
                post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validLoginRequest)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    LoginResponseDto firstLoginResponse =
        objectMapper.readValue(firstLoginBody, LoginResponseDto.class);

    String secondLoginBody =
        mockMvc
            .perform(
                post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validLoginRequest)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    LoginResponseDto secondLoginResponse =
        objectMapper.readValue(secondLoginBody, LoginResponseDto.class);
    assertNotEquals(firstLoginResponse.getRefreshToken(), secondLoginResponse.getRefreshToken());

    mockMvc
        .perform(
            post(REFRESH_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new RefreshTokenRequestDto(firstLoginResponse.getRefreshToken()))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("TYCHE-103"))
        .andExpect(jsonPath("$.type").value("AUTH_REFRESH_TOKEN_INVALID"));

    mockMvc
        .perform(
            post(REFRESH_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new RefreshTokenRequestDto(secondLoginResponse.getRefreshToken()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.refreshToken").isString());
  }

  @ParameterizedTest
  @MethodSource("com.tychewealth.testdata.AuthTestData#invalidLoginRequests")
  void loginReturnsBadRequestForInvalidPayload(
      LoginRequestDto invalidRequest, String expectedMessage) throws Exception {
    mockMvc
        .perform(
            post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("TYCHE-002"))
        .andExpect(jsonPath("$.type").value("GENERIC_VALIDATION_ERROR"))
        .andExpect(jsonPath("$.description").value(containsString(expectedMessage)));
  }

  @Test
  void refreshRotatesTokensWhenRefreshTokenIsValid() throws Exception {
    UserEntity user = userRepository.save(buildUser("refresh.user@tychewealth.com", "refreshuser"));
    String previousTokenValue = "existing-refresh-token";
    RefreshTokenEntity previousToken =
        refreshTokenRepository.save(
            buildRefreshToken(previousTokenValue, user, Instant.now().plusSeconds(3600), false));

    String responseBody =
        mockMvc
            .perform(
                post(REFRESH_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper.writeValueAsString(
                            new RefreshTokenRequestDto(previousTokenValue))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.expiresAt").exists())
            .andExpect(jsonPath("$.refreshToken").isString())
            .andReturn()
            .getResponse()
            .getContentAsString();

    RefreshTokenResponseDto refreshResponse =
        objectMapper.readValue(responseBody, RefreshTokenResponseDto.class);

    assertNotEquals(previousTokenValue, refreshResponse.getRefreshToken());
    assertTrue(refreshTokenRepository.findByToken(refreshResponse.getRefreshToken()).isPresent());
    assertFalse(
        refreshTokenRepository
            .findByToken(refreshResponse.getRefreshToken())
            .orElseThrow()
            .isRevoked());
    assertTrue(refreshTokenRepository.findByToken(previousTokenValue).orElseThrow().isRevoked());
    assertEquals(
        previousToken.getUser().getId(),
        refreshTokenRepository
            .findByToken(refreshResponse.getRefreshToken())
            .orElseThrow()
            .getUser()
            .getId());
  }

  @Test
  void refreshReturnsUnauthorizedWhenRefreshTokenDoesNotExist() throws Exception {
    mockMvc
        .perform(
            post(REFRESH_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(new RefreshTokenRequestDto("missing-token"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("TYCHE-103"))
        .andExpect(jsonPath("$.type").value("AUTH_REFRESH_TOKEN_INVALID"))
        .andExpect(jsonPath("$.description").value("The provided refresh token is invalid"));
  }

  @Test
  void refreshReturnsUnauthorizedWhenRefreshTokenIsRevoked() throws Exception {
    UserEntity user =
        userRepository.save(buildUser("refresh.revoked@tychewealth.com", "refreshrevoked"));
    refreshTokenRepository.save(
        buildRefreshToken("revoked-refresh-token", user, Instant.now().plusSeconds(3600), true));

    mockMvc
        .perform(
            post(REFRESH_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new RefreshTokenRequestDto("revoked-refresh-token"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("TYCHE-103"))
        .andExpect(jsonPath("$.type").value("AUTH_REFRESH_TOKEN_INVALID"))
        .andExpect(jsonPath("$.description").value("The provided refresh token is invalid"));
  }

  @Test
  void refreshReturnsUnauthorizedWhenRefreshTokenIsExpired() throws Exception {
    UserEntity user =
        userRepository.save(buildUser("refresh.expired@tychewealth.com", "refreshexpired"));
    refreshTokenRepository.save(
        buildRefreshToken("expired-refresh-token", user, Instant.now().minusSeconds(5), false));

    mockMvc
        .perform(
            post(REFRESH_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new RefreshTokenRequestDto("expired-refresh-token"))))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("TYCHE-103"))
        .andExpect(jsonPath("$.type").value("AUTH_REFRESH_TOKEN_INVALID"))
        .andExpect(jsonPath("$.description").value("The provided refresh token is invalid"));
  }

  @Test
  void refreshReturnsBadRequestWhenPayloadIsInvalid() throws Exception {
    mockMvc
        .perform(
            post(REFRESH_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\" \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("TYCHE-002"))
        .andExpect(jsonPath("$.type").value("GENERIC_VALIDATION_ERROR"));
  }

  @Test
  void refreshReturnsBadRequestWhenRequestBodyIsEmpty() throws Exception {
    mockMvc
        .perform(post(REFRESH_URL).contentType(MediaType.APPLICATION_JSON).content(""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("TYCHE-003"))
        .andExpect(jsonPath("$.type").value("GENERIC_BAD_REQUEST"));
  }
}

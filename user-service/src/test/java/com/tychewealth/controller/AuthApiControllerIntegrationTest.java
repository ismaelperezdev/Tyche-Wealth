package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.AUTH_LOGIN_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_REFRESH_URL;
import static com.tychewealth.constants.ApiConstants.AUTH_REGISTER_URL;
import static com.tychewealth.constants.AuthConstants.LOGIN_RATE_LIMIT_MESSAGE;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_LOGIN_FAILURE;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_LOGIN_INVALID_CREDENTIALS;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_LOGIN_RATE_LIMITED;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_LOGIN_REQUESTS;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_LOGIN_SUCCESS;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_REFRESH_RATE_LIMITED;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_REFRESH_REQUESTS;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_REFRESH_SUCCESS;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_REFRESH_TOKEN_ISSUED;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_REFRESH_TOKEN_REVOKED;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_REGISTER_CONFLICT;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_REGISTER_FAILURE;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_REGISTER_RATE_LIMITED;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_REGISTER_REQUESTS;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_REGISTER_SUCCESS;
import static com.tychewealth.constants.AuthConstants.REFRESH_RATE_LIMIT_MESSAGE;
import static com.tychewealth.constants.AuthConstants.REGISTER_RATE_LIMIT_MESSAGE;
import static com.tychewealth.constants.AuthConstants.TOKEN_TYPE_BEARER;
import static com.tychewealth.testdata.EntityBuilder.buildRefreshToken;
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
import com.tychewealth.config.RefreshRateLimitConfig;
import com.tychewealth.dto.user.LoginResponseDto;
import com.tychewealth.dto.user.RefreshTokenResponseDto;
import com.tychewealth.dto.user.request.LoginRequestDto;
import com.tychewealth.dto.user.request.RefreshTokenRequestDto;
import com.tychewealth.dto.user.request.RegisterRequestDto;
import com.tychewealth.entity.RefreshTokenEntity;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.RefreshTokenRepository;
import com.tychewealth.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(classes = AuthIntegrationTestConfig.class)
@ContextConfiguration(initializers = AuthIntegrationTestConfig.Initializer.class)
@AutoConfigureMockMvc
class AuthApiControllerIntegrationTest {

  private static final String MISSING_REFRESH_TOKEN = "missing-token";
  private static final String EXISTING_REFRESH_TOKEN = "existing-refresh-token";
  private static final String REVOKED_REFRESH_TOKEN = "revoked-refresh-token";
  private static final String EXPIRED_REFRESH_TOKEN = "expired-refresh-token";
  private static final String METRICS_REFRESH_TOKEN = "metrics-refresh-token";
  private static final String VALID_PASSWORD = "Secret123!";
  private static final String INVALID_PASSWORD = "Wrong123!";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private UserRepository userRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private MeterRegistry meterRegistry;
  @Autowired private RefreshRateLimitConfig rateLimitConfig;

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
    rateLimitConfig.resetAll();
    validRequest =
        new RegisterRequestDto("laura.gomez@tychewealth.com", "lauragomez", VALID_PASSWORD);
    conflictByEmailRequest =
        new RegisterRequestDto(validRequest.getEmail(), "carlosmartin", VALID_PASSWORD);
    conflictByUsernameRequest =
        new RegisterRequestDto(
            "pablo.ortega@tychewealth.com", validRequest.getUsername(), VALID_PASSWORD);

    existingEmailUser = new UserEntity();
    existingEmailUser.setEmail(validRequest.getEmail());
    existingEmailUser.setUsername("anabelruiz");
    existingEmailUser.setPassword(passwordEncoder.encode(VALID_PASSWORD));

    existingUsernameUser = new UserEntity();
    existingUsernameUser.setEmail("mario.santos@tychewealth.com");
    existingUsernameUser.setUsername(validRequest.getUsername());
    existingUsernameUser.setPassword(passwordEncoder.encode(VALID_PASSWORD));

    existingLoginUser = new UserEntity();
    existingLoginUser.setEmail(validRequest.getEmail());
    existingLoginUser.setUsername(validRequest.getUsername());
    existingLoginUser.setPassword(passwordEncoder.encode(validRequest.getPassword()));

    validLoginRequest = new LoginRequestDto(validRequest.getEmail(), validRequest.getPassword());
  }

  @Test
  void registerCreatesUserWhenRequestIsValid() throws Exception {
    double requestsBefore = counterValue(METRIC_AUTH_REGISTER_REQUESTS);
    double successBefore = counterValue(METRIC_AUTH_REGISTER_SUCCESS);

    registerRequest(validRequest)
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
    assertEquals(requestsBefore + 1, counterValue(METRIC_AUTH_REGISTER_REQUESTS));
    assertEquals(successBefore + 1, counterValue(METRIC_AUTH_REGISTER_SUCCESS));
  }

  @Test
  void registerReturnsConflictWhenEmailAlreadyExists() throws Exception {
    userRepository.save(existingEmailUser);
    double failureBefore = counterValue(METRIC_AUTH_REGISTER_FAILURE);
    double conflictBefore = counterValue(METRIC_AUTH_REGISTER_CONFLICT);

    registerRequest(conflictByEmailRequest)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.AUTH_REGISTRATION_CONFLICT.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.AUTH_REGISTRATION_CONFLICT.getType()))
        .andExpect(
            jsonPath("$.description")
                .value(ErrorDefinition.AUTH_REGISTRATION_CONFLICT.getDescription()));

    assertEquals(failureBefore + 1, counterValue(METRIC_AUTH_REGISTER_FAILURE));
    assertEquals(conflictBefore + 1, counterValue(METRIC_AUTH_REGISTER_CONFLICT));
  }

  @Test
  void registerReturnsConflictWhenUsernameAlreadyExists() throws Exception {
    userRepository.save(existingUsernameUser);

    registerRequest(conflictByUsernameRequest)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.AUTH_REGISTRATION_CONFLICT.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.AUTH_REGISTRATION_CONFLICT.getType()))
        .andExpect(
            jsonPath("$.description")
                .value(ErrorDefinition.AUTH_REGISTRATION_CONFLICT.getDescription()));
  }

  @Test
  void registerReturnsTooManyRequestsWhenRateLimitIsExceeded() throws Exception {
    RegisterRequestDto invalidRegisterRequest = new RegisterRequestDto("", "", "short");
    double requestsBefore = counterValue(METRIC_AUTH_REGISTER_REQUESTS);
    double rateLimitedBefore = counterValue(METRIC_AUTH_REGISTER_RATE_LIMITED);

    registerRequest(invalidRegisterRequest).andExpect(status().isBadRequest());

    registerRequest(invalidRegisterRequest).andExpect(status().isBadRequest());

    registerRequest(invalidRegisterRequest)
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.RATE_LIMITED.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.RATE_LIMITED.getType()))
        .andExpect(jsonPath("$.description").value(REGISTER_RATE_LIMIT_MESSAGE));

    assertEquals(requestsBefore + 3, counterValue(METRIC_AUTH_REGISTER_REQUESTS));
    assertEquals(rateLimitedBefore + 1, counterValue(METRIC_AUTH_REGISTER_RATE_LIMITED));
  }

  @ParameterizedTest
  @MethodSource("com.tychewealth.testdata.AuthTestData#invalidCreateRequests")
  void registerReturnsBadRequestForInvalidPayload(
      RegisterRequestDto invalidRequest, String expectedMessage) throws Exception {
    registerRequest(invalidRequest)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.GENERIC_VALIDATION_ERROR.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.GENERIC_VALIDATION_ERROR.getType()))
        .andExpect(jsonPath("$.description").value(containsString(expectedMessage)));
  }

  @Test
  void loginReturnsTokenAndUserWhenCredentialsAreValid() throws Exception {
    userRepository.save(existingLoginUser);
    double requestsBefore = counterValue(METRIC_AUTH_LOGIN_REQUESTS);
    double successBefore = counterValue(METRIC_AUTH_LOGIN_SUCCESS);

    loginRequest(validLoginRequest)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tokenType").value(TOKEN_TYPE_BEARER))
        .andExpect(jsonPath("$.accessToken").isString())
        .andExpect(jsonPath("$.refreshToken").isString())
        .andExpect(jsonPath("$.expiresIn").isNumber())
        .andExpect(jsonPath("$.user.id").isNumber())
        .andExpect(jsonPath("$.user.email").value(validRequest.getEmail()))
        .andExpect(jsonPath("$.user.username").value(validRequest.getUsername()))
        .andExpect(jsonPath("$.user.password").doesNotExist());

    assertEquals(requestsBefore + 1, counterValue(METRIC_AUTH_LOGIN_REQUESTS));
    assertEquals(successBefore + 1, counterValue(METRIC_AUTH_LOGIN_SUCCESS));
  }

  @Test
  void loginReturnsUnauthorizedWhenCredentialsAreInvalid() throws Exception {
    userRepository.save(existingLoginUser);
    double failureBefore = counterValue(METRIC_AUTH_LOGIN_FAILURE);
    double invalidCredentialsBefore = counterValue(METRIC_AUTH_LOGIN_INVALID_CREDENTIALS);

    LoginRequestDto invalidLoginRequest =
        new LoginRequestDto(validRequest.getEmail(), INVALID_PASSWORD);

    loginRequest(invalidLoginRequest)
        .andExpect(status().isUnauthorized())
        .andExpect(
            jsonPath("$.code").value(ErrorDefinition.AUTH_LOGIN_INVALID_CREDENTIALS.getCode()))
        .andExpect(
            jsonPath("$.type").value(ErrorDefinition.AUTH_LOGIN_INVALID_CREDENTIALS.getType()))
        .andExpect(
            jsonPath("$.description")
                .value(ErrorDefinition.AUTH_LOGIN_INVALID_CREDENTIALS.getDescription()));

    assertEquals(failureBefore + 1, counterValue(METRIC_AUTH_LOGIN_FAILURE));
    assertEquals(invalidCredentialsBefore + 1, counterValue(METRIC_AUTH_LOGIN_INVALID_CREDENTIALS));
  }

  @Test
  void loginReturnsTooManyRequestsWhenRateLimitIsExceeded() throws Exception {
    userRepository.save(existingLoginUser);
    double requestsBefore = counterValue(METRIC_AUTH_LOGIN_REQUESTS);
    double rateLimitedBefore = counterValue(METRIC_AUTH_LOGIN_RATE_LIMITED);

    LoginRequestDto invalidLoginRequest =
        new LoginRequestDto(validRequest.getEmail(), INVALID_PASSWORD);

    loginRequest(invalidLoginRequest).andExpect(status().isUnauthorized());

    loginRequest(invalidLoginRequest).andExpect(status().isUnauthorized());

    loginRequest(invalidLoginRequest)
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.RATE_LIMITED.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.RATE_LIMITED.getType()))
        .andExpect(jsonPath("$.description").value(LOGIN_RATE_LIMIT_MESSAGE));

    assertEquals(requestsBefore + 3, counterValue(METRIC_AUTH_LOGIN_REQUESTS));
    assertEquals(rateLimitedBefore + 1, counterValue(METRIC_AUTH_LOGIN_RATE_LIMITED));
  }

  @Test
  void secondLoginRevokesPreviousRefreshToken() throws Exception {
    userRepository.save(existingLoginUser);

    LoginResponseDto firstLoginResponse = login(validLoginRequest);
    LoginResponseDto secondLoginResponse = login(validLoginRequest);
    assertNotEquals(firstLoginResponse.getRefreshToken(), secondLoginResponse.getRefreshToken());

    refresh(firstLoginResponse.getRefreshToken())
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID.getType()));

    refresh(secondLoginResponse.getRefreshToken())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.refreshToken").isString());
  }

  @ParameterizedTest
  @MethodSource("com.tychewealth.testdata.AuthTestData#invalidLoginRequests")
  void loginReturnsBadRequestForInvalidPayload(
      LoginRequestDto invalidRequest, String expectedMessage) throws Exception {
    loginRequest(invalidRequest)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.GENERIC_VALIDATION_ERROR.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.GENERIC_VALIDATION_ERROR.getType()))
        .andExpect(jsonPath("$.description").value(containsString(expectedMessage)));
  }

  @Test
  void refreshRotatesTokensWhenRefreshTokenIsValid() throws Exception {
    UserEntity user = userRepository.save(existingLoginUser);
    String previousTokenValue = EXISTING_REFRESH_TOKEN;
    RefreshTokenEntity previousToken =
        refreshTokenRepository.save(
            buildRefreshToken(previousTokenValue, user, Instant.now().plusSeconds(3600), false));

    String responseBody =
        refresh(previousTokenValue)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenType").value(TOKEN_TYPE_BEARER))
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.expiresIn").isNumber())
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
    refresh(MISSING_REFRESH_TOKEN)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID.getType()))
        .andExpect(
            jsonPath("$.description")
                .value(ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID.getDescription()));
  }

  @Test
  void refreshReturnsUnauthorizedWhenRefreshTokenIsRevoked() throws Exception {
    UserEntity user = userRepository.save(existingLoginUser);
    refreshTokenRepository.save(
        buildRefreshToken(REVOKED_REFRESH_TOKEN, user, Instant.now().plusSeconds(3600), true));

    refresh(REVOKED_REFRESH_TOKEN)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID.getType()))
        .andExpect(
            jsonPath("$.description")
                .value(ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID.getDescription()));
  }

  @Test
  void refreshReturnsUnauthorizedWhenRefreshTokenIsExpired() throws Exception {
    UserEntity user = userRepository.save(existingLoginUser);
    refreshTokenRepository.save(
        buildRefreshToken(EXPIRED_REFRESH_TOKEN, user, Instant.now().minusSeconds(5), false));

    refresh(EXPIRED_REFRESH_TOKEN)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID.getType()))
        .andExpect(
            jsonPath("$.description")
                .value(ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID.getDescription()));
  }

  @Test
  void refreshReturnsBadRequestWhenPayloadIsInvalid() throws Exception {
    refresh(" ")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.GENERIC_VALIDATION_ERROR.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.GENERIC_VALIDATION_ERROR.getType()));
  }

  @Test
  void refreshReturnsBadRequestWhenRequestBodyIsEmpty() throws Exception {
    mockMvc
        .perform(post(AUTH_REFRESH_URL).contentType(MediaType.APPLICATION_JSON).content(""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.GENERIC_BAD_REQUEST.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.GENERIC_BAD_REQUEST.getType()));
  }

  @Test
  void refreshRecordsRequestSuccessAndTokenLifecycleMetrics() throws Exception {
    UserEntity user = userRepository.save(existingLoginUser);
    String previousTokenValue = METRICS_REFRESH_TOKEN;
    refreshTokenRepository.save(
        buildRefreshToken(previousTokenValue, user, Instant.now().plusSeconds(3600), false));

    double requestsBefore = counterValue(METRIC_AUTH_REFRESH_REQUESTS);
    double successBefore = counterValue(METRIC_AUTH_REFRESH_SUCCESS);
    double issuedBefore = counterValue(METRIC_AUTH_REFRESH_TOKEN_ISSUED);
    double revokedBefore = counterValue(METRIC_AUTH_REFRESH_TOKEN_REVOKED);

    refresh(previousTokenValue).andExpect(status().isOk());

    assertEquals(requestsBefore + 1, counterValue(METRIC_AUTH_REFRESH_REQUESTS));
    assertEquals(successBefore + 1, counterValue(METRIC_AUTH_REFRESH_SUCCESS));
    assertEquals(issuedBefore + 1, counterValue(METRIC_AUTH_REFRESH_TOKEN_ISSUED));
    assertEquals(revokedBefore + 1, counterValue(METRIC_AUTH_REFRESH_TOKEN_REVOKED));
  }

  @Test
  void refreshReturnsTooManyRequestsWhenRateLimitIsExceeded() throws Exception {
    double requestsBefore = counterValue(METRIC_AUTH_REFRESH_REQUESTS);
    double rateLimitedBefore = counterValue(METRIC_AUTH_REFRESH_RATE_LIMITED);

    refresh(MISSING_REFRESH_TOKEN).andExpect(status().isUnauthorized());

    refresh(MISSING_REFRESH_TOKEN).andExpect(status().isUnauthorized());

    refresh(MISSING_REFRESH_TOKEN)
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.RATE_LIMITED.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.RATE_LIMITED.getType()))
        .andExpect(jsonPath("$.description").value(REFRESH_RATE_LIMIT_MESSAGE));

    assertEquals(requestsBefore + 3, counterValue(METRIC_AUTH_REFRESH_REQUESTS));
    assertEquals(rateLimitedBefore + 1, counterValue(METRIC_AUTH_REFRESH_RATE_LIMITED));
  }

  private double counterValue(String counterName) {
    var counter = meterRegistry.find(counterName).counter();
    return counter == null ? 0 : counter.count();
  }

  private LoginResponseDto login(LoginRequestDto request) throws Exception {
    String responseBody =
        loginRequest(request)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    return objectMapper.readValue(responseBody, LoginResponseDto.class);
  }

  private ResultActions registerRequest(RegisterRequestDto request) throws Exception {
    return mockMvc.perform(
        post(AUTH_REGISTER_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));
  }

  private ResultActions loginRequest(LoginRequestDto request) throws Exception {
    return mockMvc.perform(
        post(AUTH_LOGIN_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)));
  }

  private ResultActions refresh(String refreshToken) throws Exception {
    return mockMvc.perform(
        post(AUTH_REFRESH_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new RefreshTokenRequestDto(refreshToken))));
  }
}

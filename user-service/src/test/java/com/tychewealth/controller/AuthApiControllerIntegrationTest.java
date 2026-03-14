package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.AUTH_REFRESH_URL;
import static com.tychewealth.constants.AuthConstants.TOKEN_TYPE_BEARER;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_LOGIN_FAILURE;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_LOGIN_INVALID_CREDENTIALS;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_LOGIN_RATE_LIMITED;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_LOGIN_REQUESTS;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_LOGIN_SUCCESS;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REFRESH_RATE_LIMITED;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REFRESH_REQUESTS;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REFRESH_SUCCESS;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REFRESH_TOKEN_ISSUED;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REFRESH_TOKEN_REVOKED;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REGISTER_CONFLICT;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REGISTER_FAILURE;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REGISTER_RATE_LIMITED;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REGISTER_REQUESTS;
import static com.tychewealth.constants.MetricConstants.METRIC_AUTH_REGISTER_SUCCESS;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_INVALID;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_VALID;
import static com.tychewealth.constants.TestConstants.TEST_REFRESH_TOKEN_EXISTING;
import static com.tychewealth.constants.TestConstants.TEST_REFRESH_TOKEN_EXPIRED;
import static com.tychewealth.constants.TestConstants.TEST_REFRESH_TOKEN_METRICS;
import static com.tychewealth.constants.TestConstants.TEST_REFRESH_TOKEN_MISSING;
import static com.tychewealth.constants.TestConstants.TEST_REFRESH_TOKEN_REVOKED;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_LAURA;
import static com.tychewealth.testdata.EntityBuilder.buildRefreshToken;
import static com.tychewealth.testhelper.AuthTestHelper.login;
import static com.tychewealth.testhelper.AuthTestHelper.loginRequest;
import static com.tychewealth.testhelper.AuthTestHelper.logout;
import static com.tychewealth.testhelper.AuthTestHelper.refresh;
import static com.tychewealth.testhelper.AuthTestHelper.registerRequest;
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
import com.tychewealth.dto.auth.LoginResponseDto;
import com.tychewealth.dto.auth.RefreshTokenResponseDto;
import com.tychewealth.dto.auth.request.LoginRequestDto;
import com.tychewealth.dto.auth.request.RegisterRequestDto;
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

@SpringBootTest(classes = AuthIntegrationTestConfig.class)
@ContextConfiguration(initializers = AuthIntegrationTestConfig.Initializer.class)
@AutoConfigureMockMvc
class AuthApiControllerIntegrationTest {

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
        new RegisterRequestDto(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_PASSWORD_VALID);
    conflictByEmailRequest =
        new RegisterRequestDto(validRequest.getEmail(), "carlosmartin", TEST_PASSWORD_VALID);
    conflictByUsernameRequest =
        new RegisterRequestDto(
            "pablo.ortega@tychewealth.com", validRequest.getUsername(), TEST_PASSWORD_VALID);

    existingEmailUser = new UserEntity();
    existingEmailUser.setEmail(validRequest.getEmail());
    existingEmailUser.setUsername("anabelruiz");
    existingEmailUser.setPassword(passwordEncoder.encode(TEST_PASSWORD_VALID));

    existingUsernameUser = new UserEntity();
    existingUsernameUser.setEmail("mario.santos@tychewealth.com");
    existingUsernameUser.setUsername(validRequest.getUsername());
    existingUsernameUser.setPassword(passwordEncoder.encode(TEST_PASSWORD_VALID));

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

    registerRequest(mockMvc, objectMapper, validRequest)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.email").value(validRequest.getEmail()))
        .andExpect(jsonPath("$.username").value(validRequest.getUsername()))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.password").doesNotExist());

    UserEntity created =
        userRepository.findByEmailIncludingDeleted(validRequest.getEmail()).orElseThrow();
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

    registerRequest(mockMvc, objectMapper, conflictByEmailRequest)
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

    registerRequest(mockMvc, objectMapper, conflictByUsernameRequest)
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

    registerRequest(mockMvc, objectMapper, invalidRegisterRequest)
        .andExpect(status().isBadRequest());

    registerRequest(mockMvc, objectMapper, invalidRegisterRequest)
        .andExpect(status().isBadRequest());

    registerRequest(mockMvc, objectMapper, invalidRegisterRequest)
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.RATE_LIMITED.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.RATE_LIMITED.getType()))
        .andExpect(jsonPath("$.description").value(ErrorDefinition.RATE_LIMITED.getDescription()));

    assertEquals(requestsBefore + 3, counterValue(METRIC_AUTH_REGISTER_REQUESTS));
    assertEquals(rateLimitedBefore + 1, counterValue(METRIC_AUTH_REGISTER_RATE_LIMITED));
  }

  @ParameterizedTest
  @MethodSource("com.tychewealth.testdata.AuthTestData#invalidCreateRequests")
  void registerReturnsBadRequestForInvalidPayload(
      RegisterRequestDto invalidRequest, String expectedMessage) throws Exception {
    registerRequest(mockMvc, objectMapper, invalidRequest)
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

    loginRequest(mockMvc, objectMapper, validLoginRequest)
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
        new LoginRequestDto(validRequest.getEmail(), TEST_PASSWORD_INVALID);

    loginRequest(mockMvc, objectMapper, invalidLoginRequest)
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
  void loginReturnsUnauthorizedWhenUserWasSoftDeleted() throws Exception {
    existingLoginUser.setDeletedAt(java.time.LocalDateTime.now());
    userRepository.save(existingLoginUser);

    loginRequest(mockMvc, objectMapper, validLoginRequest)
        .andExpect(status().isUnauthorized())
        .andExpect(
            jsonPath("$.code").value(ErrorDefinition.AUTH_LOGIN_INVALID_CREDENTIALS.getCode()))
        .andExpect(
            jsonPath("$.type").value(ErrorDefinition.AUTH_LOGIN_INVALID_CREDENTIALS.getType()))
        .andExpect(
            jsonPath("$.description")
                .value(ErrorDefinition.AUTH_LOGIN_INVALID_CREDENTIALS.getDescription()));
  }

  @Test
  void loginReturnsTooManyRequestsWhenRateLimitIsExceeded() throws Exception {
    userRepository.save(existingLoginUser);
    double requestsBefore = counterValue(METRIC_AUTH_LOGIN_REQUESTS);
    double rateLimitedBefore = counterValue(METRIC_AUTH_LOGIN_RATE_LIMITED);

    LoginRequestDto invalidLoginRequest =
        new LoginRequestDto(validRequest.getEmail(), TEST_PASSWORD_INVALID);

    loginRequest(mockMvc, objectMapper, invalidLoginRequest).andExpect(status().isUnauthorized());

    loginRequest(mockMvc, objectMapper, invalidLoginRequest).andExpect(status().isUnauthorized());

    loginRequest(mockMvc, objectMapper, invalidLoginRequest)
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.RATE_LIMITED.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.RATE_LIMITED.getType()))
        .andExpect(jsonPath("$.description").value(ErrorDefinition.RATE_LIMITED.getDescription()));

    assertEquals(requestsBefore + 3, counterValue(METRIC_AUTH_LOGIN_REQUESTS));
    assertEquals(rateLimitedBefore + 1, counterValue(METRIC_AUTH_LOGIN_RATE_LIMITED));
  }

  @Test
  void secondLoginRevokesPreviousRefreshToken() throws Exception {
    userRepository.save(existingLoginUser);

    LoginResponseDto firstLoginResponse = login(mockMvc, objectMapper, validLoginRequest);
    LoginResponseDto secondLoginResponse = login(mockMvc, objectMapper, validLoginRequest);
    assertNotEquals(firstLoginResponse.getRefreshToken(), secondLoginResponse.getRefreshToken());

    refresh(mockMvc, objectMapper, firstLoginResponse.getRefreshToken())
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID.getType()));

    refresh(mockMvc, objectMapper, secondLoginResponse.getRefreshToken())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.refreshToken").isString());
  }

  @ParameterizedTest
  @MethodSource("com.tychewealth.testdata.AuthTestData#invalidLoginRequests")
  void loginReturnsBadRequestForInvalidPayload(
      LoginRequestDto invalidRequest, String expectedMessage) throws Exception {
    loginRequest(mockMvc, objectMapper, invalidRequest)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.GENERIC_VALIDATION_ERROR.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.GENERIC_VALIDATION_ERROR.getType()))
        .andExpect(jsonPath("$.description").value(containsString(expectedMessage)));
  }

  @Test
  void refreshRotatesTokensWhenRefreshTokenIsValid() throws Exception {
    UserEntity user = userRepository.save(existingLoginUser);
    String previousTokenValue = TEST_REFRESH_TOKEN_EXISTING;
    RefreshTokenEntity previousToken =
        refreshTokenRepository.save(
            buildRefreshToken(previousTokenValue, user, Instant.now().plusSeconds(3600), false));

    String responseBody =
        refresh(mockMvc, objectMapper, previousTokenValue)
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
    refresh(mockMvc, objectMapper, TEST_REFRESH_TOKEN_MISSING)
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
        buildRefreshToken(TEST_REFRESH_TOKEN_REVOKED, user, Instant.now().plusSeconds(3600), true));

    refresh(mockMvc, objectMapper, TEST_REFRESH_TOKEN_REVOKED)
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
        buildRefreshToken(TEST_REFRESH_TOKEN_EXPIRED, user, Instant.now().minusSeconds(5), false));

    refresh(mockMvc, objectMapper, TEST_REFRESH_TOKEN_EXPIRED)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID.getType()))
        .andExpect(
            jsonPath("$.description")
                .value(ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID.getDescription()));
  }

  @Test
  void refreshReturnsBadRequestWhenPayloadIsInvalid() throws Exception {
    refresh(mockMvc, objectMapper, " ")
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
    String previousTokenValue = TEST_REFRESH_TOKEN_METRICS;
    refreshTokenRepository.save(
        buildRefreshToken(previousTokenValue, user, Instant.now().plusSeconds(3600), false));

    double requestsBefore = counterValue(METRIC_AUTH_REFRESH_REQUESTS);
    double successBefore = counterValue(METRIC_AUTH_REFRESH_SUCCESS);
    double issuedBefore = counterValue(METRIC_AUTH_REFRESH_TOKEN_ISSUED);
    double revokedBefore = counterValue(METRIC_AUTH_REFRESH_TOKEN_REVOKED);

    refresh(mockMvc, objectMapper, previousTokenValue).andExpect(status().isOk());

    assertEquals(requestsBefore + 1, counterValue(METRIC_AUTH_REFRESH_REQUESTS));
    assertEquals(successBefore + 1, counterValue(METRIC_AUTH_REFRESH_SUCCESS));
    assertEquals(issuedBefore + 1, counterValue(METRIC_AUTH_REFRESH_TOKEN_ISSUED));
    assertEquals(revokedBefore + 1, counterValue(METRIC_AUTH_REFRESH_TOKEN_REVOKED));
  }

  @Test
  void refreshReturnsTooManyRequestsWhenRateLimitIsExceeded() throws Exception {
    double requestsBefore = counterValue(METRIC_AUTH_REFRESH_REQUESTS);
    double rateLimitedBefore = counterValue(METRIC_AUTH_REFRESH_RATE_LIMITED);

    refresh(mockMvc, objectMapper, TEST_REFRESH_TOKEN_MISSING).andExpect(status().isUnauthorized());

    refresh(mockMvc, objectMapper, TEST_REFRESH_TOKEN_MISSING).andExpect(status().isUnauthorized());

    refresh(mockMvc, objectMapper, TEST_REFRESH_TOKEN_MISSING)
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.RATE_LIMITED.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.RATE_LIMITED.getType()))
        .andExpect(jsonPath("$.description").value(ErrorDefinition.RATE_LIMITED.getDescription()));

    assertEquals(requestsBefore + 3, counterValue(METRIC_AUTH_REFRESH_REQUESTS));
    assertEquals(rateLimitedBefore + 1, counterValue(METRIC_AUTH_REFRESH_RATE_LIMITED));
  }

  @Test
  void logoutRevokesRefreshTokenWhenTokenIsValid() throws Exception {
    UserEntity user = userRepository.save(existingLoginUser);
    refreshTokenRepository.save(
        buildRefreshToken(
            TEST_REFRESH_TOKEN_EXISTING, user, Instant.now().plusSeconds(3600), false));

    logout(mockMvc, objectMapper, TEST_REFRESH_TOKEN_EXISTING).andExpect(status().isNoContent());

    assertTrue(
        refreshTokenRepository.findByToken(TEST_REFRESH_TOKEN_EXISTING).orElseThrow().isRevoked());
  }

  @Test
  void logoutReturnsUnauthorizedWhenRefreshTokenDoesNotExist() throws Exception {
    logout(mockMvc, objectMapper, TEST_REFRESH_TOKEN_MISSING)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID.getType()))
        .andExpect(
            jsonPath("$.description")
                .value(ErrorDefinition.AUTH_REFRESH_TOKEN_INVALID.getDescription()));
  }

  @Test
  void logoutReturnsBadRequestWhenPayloadIsInvalid() throws Exception {
    logout(mockMvc, objectMapper, " ")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.GENERIC_VALIDATION_ERROR.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.GENERIC_VALIDATION_ERROR.getType()));
  }

  private double counterValue(String counterName) {
    var counter = meterRegistry.find(counterName).counter();
    return counter == null ? 0 : counter.count();
  }
}

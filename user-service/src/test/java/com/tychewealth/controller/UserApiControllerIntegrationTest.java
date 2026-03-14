package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.USER_ME_PASSWORD_URL;
import static com.tychewealth.constants.ApiConstants.USER_ME_URL;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_OCCUPIED_USERNAME;
import static com.tychewealth.constants.TestConstants.TEST_OTHER_EMAIL;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_CONFIRM_MISMATCH;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_INVALID;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_NEW_VALID;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_VALID;
import static com.tychewealth.constants.TestConstants.TEST_UPDATE_USERNAME_NORMALIZED;
import static com.tychewealth.constants.TestConstants.TEST_UPDATE_USERNAME_REQUEST;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_LAURA;
import static com.tychewealth.testdata.EntityBuilder.buildRefreshToken;
import static com.tychewealth.testdata.EntityBuilder.buildUser;
import static com.tychewealth.testhelper.UserTestHelper.deleteRequest;
import static com.tychewealth.testhelper.UserTestHelper.deleteRequestUnauthorized;
import static com.tychewealth.testhelper.UserTestHelper.passwordUpdateRequestBody;
import static com.tychewealth.testhelper.UserTestHelper.retrieveRequest;
import static com.tychewealth.testhelper.UserTestHelper.retrieveRequestUnauthorized;
import static com.tychewealth.testhelper.UserTestHelper.updatePasswordRequest;
import static com.tychewealth.testhelper.UserTestHelper.updatePasswordRequestUnauthorized;
import static com.tychewealth.testhelper.UserTestHelper.updateRequest;
import static com.tychewealth.testhelper.UserTestHelper.updateRequestUnauthorized;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.config.UserIntegrationTestConfig;
import com.tychewealth.entity.RefreshTokenEntity;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.RefreshTokenRepository;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.helper.AuthTokenHelper;
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

@SpringBootTest(classes = UserIntegrationTestConfig.class)
@ContextConfiguration(initializers = UserIntegrationTestConfig.Initializer.class)
@AutoConfigureMockMvc
class UserApiControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private AuthTokenHelper authTokenHelper;
  @Autowired private PasswordEncoder passwordEncoder;

  private UserEntity existingUser;

  @BeforeEach
  void setUp() {
    refreshTokenRepository.deleteAll();
    userRepository.deleteAll();
    existingUser =
        buildUser(
            TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, passwordEncoder.encode(TEST_PASSWORD_VALID));
  }

  @Test
  void retrieveReturnsUserWhenUserExists() throws Exception {
    UserEntity saved = userRepository.save(existingUser);
    String accessToken = authTokenHelper.generateAccessToken(saved).accessToken();

    retrieveRequest(mockMvc, accessToken)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(saved.getId()))
        .andExpect(jsonPath("$.email").value(saved.getEmail()))
        .andExpect(jsonPath("$.username").value(saved.getUsername()))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.password").doesNotExist());
  }

  @Test
  void retrieveReturnsUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
    retrieveRequestUnauthorized(mockMvc)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.UNAUTHORIZED.getType()))
        .andExpect(jsonPath("$.description").value(ErrorDefinition.UNAUTHORIZED.getDescription()));
  }

  @Test
  void updateChangesUsernameWhenUserIsAuthenticated() throws Exception {
    UserEntity saved = userRepository.save(existingUser);
    String accessToken = authTokenHelper.generateAccessToken(saved).accessToken();

    updateRequest(mockMvc, objectMapper, accessToken, TEST_UPDATE_USERNAME_REQUEST)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(saved.getId()))
        .andExpect(jsonPath("$.email").value(saved.getEmail()))
        .andExpect(jsonPath("$.username").value(TEST_UPDATE_USERNAME_NORMALIZED));

    UserEntity updatedUser = userRepository.findById(saved.getId()).orElseThrow();
    assertEquals(TEST_UPDATE_USERNAME_NORMALIZED, updatedUser.getUsername());
  }

  @Test
  void updateReturnsUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
    updateRequestUnauthorized(mockMvc, objectMapper, TEST_UPDATE_USERNAME_NORMALIZED)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.UNAUTHORIZED.getType()))
        .andExpect(jsonPath("$.description").value(ErrorDefinition.UNAUTHORIZED.getDescription()));
  }

  @ParameterizedTest
  @MethodSource("com.tychewealth.testdata.UserTestData#invalidUpdateRequests")
  void updateReturnsBadRequestForInvalidPayload(String requestBody, String expectedMessage)
      throws Exception {
    UserEntity saved = userRepository.saveAndFlush(existingUser);
    String accessToken = authTokenHelper.generateAccessToken(saved).accessToken();

    mockMvc
        .perform(
            patch(USER_ME_URL)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.GENERIC_VALIDATION_ERROR.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.GENERIC_VALIDATION_ERROR.getType()))
        .andExpect(jsonPath("$.description").value(containsString(expectedMessage)));
  }

  @Test
  void updateReturnsNotFoundWhenAuthenticatedUserWasSoftDeleted() throws Exception {
    UserEntity saved = userRepository.save(existingUser);
    saved.setDeletedAt(java.time.LocalDateTime.now());
    userRepository.save(saved);
    String accessToken = authTokenHelper.generateAccessToken(saved).accessToken();

    updateRequest(mockMvc, objectMapper, accessToken, TEST_UPDATE_USERNAME_NORMALIZED)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.USER_NOT_FOUND.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.USER_NOT_FOUND.getType()))
        .andExpect(
            jsonPath("$.description").value(ErrorDefinition.USER_NOT_FOUND.getDescription()));
  }

  @Test
  void updateReturnsConflictWhenUsernameAlreadyExists() throws Exception {
    UserEntity saved = userRepository.saveAndFlush(existingUser);
    UserEntity anotherUser =
        buildUser(
            TEST_OTHER_EMAIL, TEST_OCCUPIED_USERNAME, passwordEncoder.encode(TEST_PASSWORD_VALID));
    userRepository.saveAndFlush(anotherUser);
    String accessToken = authTokenHelper.generateAccessToken(saved).accessToken();

    updateRequest(mockMvc, objectMapper, accessToken, TEST_OCCUPIED_USERNAME)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.USER_USERNAME_CONFLICT.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.USER_USERNAME_CONFLICT.getType()))
        .andExpect(
            jsonPath("$.description")
                .value(ErrorDefinition.USER_USERNAME_CONFLICT.getDescription()));
  }

  @Test
  void updatePasswordChangesPasswordAndRevokesActiveRefreshTokens() throws Exception {
    UserEntity saved = userRepository.saveAndFlush(existingUser);
    RefreshTokenEntity refreshToken =
        refreshTokenRepository.saveAndFlush(
            buildRefreshToken(
                "user-password-change-token", saved, Instant.now().plusSeconds(300), false));
    String accessToken = authTokenHelper.generateAccessToken(saved).accessToken();

    updatePasswordRequest(
            mockMvc,
            accessToken,
            TEST_PASSWORD_VALID,
            TEST_PASSWORD_NEW_VALID,
            TEST_PASSWORD_NEW_VALID)
        .andExpect(status().isNoContent());

    UserEntity updatedUser = userRepository.findById(saved.getId()).orElseThrow();
    assertTrue(passwordEncoder.matches(TEST_PASSWORD_NEW_VALID, updatedUser.getPassword()));
    assertTrue(
        refreshTokenRepository.findByToken(refreshToken.getToken()).orElseThrow().isRevoked());
  }

  @Test
  void updatePasswordReturnsUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
    updatePasswordRequestUnauthorized(
            mockMvc, TEST_PASSWORD_VALID, TEST_PASSWORD_NEW_VALID, TEST_PASSWORD_NEW_VALID)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.UNAUTHORIZED.getType()))
        .andExpect(jsonPath("$.description").value(ErrorDefinition.UNAUTHORIZED.getDescription()));
  }

  @ParameterizedTest
  @MethodSource("com.tychewealth.testdata.UserTestData#invalidPasswordUpdateRequests")
  void updatePasswordReturnsBadRequestForInvalidPayload(String requestBody, String expectedMessage)
      throws Exception {
    UserEntity saved = userRepository.saveAndFlush(existingUser);
    String accessToken = authTokenHelper.generateAccessToken(saved).accessToken();

    mockMvc
        .perform(
            patch(USER_ME_PASSWORD_URL)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.GENERIC_VALIDATION_ERROR.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.GENERIC_VALIDATION_ERROR.getType()))
        .andExpect(jsonPath("$.description").value(containsString(expectedMessage)));
  }

  @Test
  void updatePasswordReturnsCleanValidationMessageWhenConfirmationDoesNotMatch() throws Exception {
    UserEntity saved = userRepository.saveAndFlush(existingUser);
    String accessToken = authTokenHelper.generateAccessToken(saved).accessToken();

    mockMvc
        .perform(
            patch(USER_ME_PASSWORD_URL)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    passwordUpdateRequestBody(
                        TEST_PASSWORD_VALID,
                        TEST_PASSWORD_NEW_VALID,
                        TEST_PASSWORD_CONFIRM_MISMATCH)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.GENERIC_VALIDATION_ERROR.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.GENERIC_VALIDATION_ERROR.getType()))
        .andExpect(
            jsonPath("$.description").value("New password and confirm new password must match"));
  }

  @Test
  void updatePasswordReturnsUnauthorizedWhenCurrentPasswordIsInvalid() throws Exception {
    UserEntity saved = userRepository.saveAndFlush(existingUser);
    String accessToken = authTokenHelper.generateAccessToken(saved).accessToken();

    updatePasswordRequest(
            mockMvc,
            accessToken,
            TEST_PASSWORD_INVALID,
            TEST_PASSWORD_NEW_VALID,
            TEST_PASSWORD_NEW_VALID)
        .andExpect(status().isUnauthorized())
        .andExpect(
            jsonPath("$.code").value(ErrorDefinition.USER_CURRENT_PASSWORD_INVALID.getCode()))
        .andExpect(
            jsonPath("$.type").value(ErrorDefinition.USER_CURRENT_PASSWORD_INVALID.getType()))
        .andExpect(
            jsonPath("$.description")
                .value(ErrorDefinition.USER_CURRENT_PASSWORD_INVALID.getDescription()));
  }

  @Test
  void updatePasswordReturnsNotFoundWhenAuthenticatedUserWasSoftDeleted() throws Exception {
    UserEntity saved = userRepository.saveAndFlush(existingUser);
    saved.setDeletedAt(java.time.LocalDateTime.now());
    userRepository.saveAndFlush(saved);
    String accessToken = authTokenHelper.generateAccessToken(saved).accessToken();

    updatePasswordRequest(
            mockMvc,
            accessToken,
            TEST_PASSWORD_VALID,
            TEST_PASSWORD_NEW_VALID,
            TEST_PASSWORD_NEW_VALID)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.USER_NOT_FOUND.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.USER_NOT_FOUND.getType()))
        .andExpect(
            jsonPath("$.description").value(ErrorDefinition.USER_NOT_FOUND.getDescription()));
  }

  @Test
  void updatePasswordReturnsBadRequestWhenNewPasswordMatchesCurrentPassword() throws Exception {
    UserEntity saved = userRepository.saveAndFlush(existingUser);
    String accessToken = authTokenHelper.generateAccessToken(saved).accessToken();

    updatePasswordRequest(
            mockMvc, accessToken, TEST_PASSWORD_VALID, TEST_PASSWORD_VALID, TEST_PASSWORD_VALID)
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.code").value(ErrorDefinition.USER_NEW_PASSWORD_MUST_BE_DIFFERENT.getCode()))
        .andExpect(
            jsonPath("$.type").value(ErrorDefinition.USER_NEW_PASSWORD_MUST_BE_DIFFERENT.getType()))
        .andExpect(
            jsonPath("$.description")
                .value(ErrorDefinition.USER_NEW_PASSWORD_MUST_BE_DIFFERENT.getDescription()));
  }

  @Test
  void deleteSoftDeletesUserWhenUserIsAuthenticated() throws Exception {
    UserEntity saved = userRepository.save(existingUser);
    String accessToken = authTokenHelper.generateAccessToken(saved).accessToken();

    deleteRequest(mockMvc, accessToken).andExpect(status().isNoContent());

    UserEntity deletedUser = userRepository.findById(saved.getId()).orElseThrow();
    assertNotNull(deletedUser.getDeletedAt());
    assertTrue(userRepository.findByIdAndDeletedAtIsNull(saved.getId()).isEmpty());
  }

  @Test
  void deleteReturnsUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
    deleteRequestUnauthorized(mockMvc)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.UNAUTHORIZED.getType()))
        .andExpect(jsonPath("$.description").value(ErrorDefinition.UNAUTHORIZED.getDescription()));
  }

  @Test
  void deleteReturnsNotFoundWhenAuthenticatedUserWasSoftDeleted() throws Exception {
    UserEntity saved = userRepository.save(existingUser);
    saved.setDeletedAt(java.time.LocalDateTime.now());
    userRepository.save(saved);
    String accessToken = authTokenHelper.generateAccessToken(saved).accessToken();

    deleteRequest(mockMvc, accessToken)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.USER_NOT_FOUND.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.USER_NOT_FOUND.getType()))
        .andExpect(
            jsonPath("$.description").value(ErrorDefinition.USER_NOT_FOUND.getDescription()));
  }
}

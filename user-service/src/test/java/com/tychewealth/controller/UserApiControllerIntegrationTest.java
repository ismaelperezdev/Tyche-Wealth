package com.tychewealth.controller;

import static com.tychewealth.constants.TestConstants.TEST_EMAIL_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_OCCUPIED_USERNAME;
import static com.tychewealth.constants.TestConstants.TEST_OTHER_EMAIL;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_VALID;
import static com.tychewealth.constants.TestConstants.TEST_UPDATE_USERNAME_NORMALIZED;
import static com.tychewealth.constants.TestConstants.TEST_UPDATE_USERNAME_REQUEST;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_LAURA;
import static com.tychewealth.service.helper.UserTestHelper.deleteRequest;
import static com.tychewealth.service.helper.UserTestHelper.deleteRequestUnauthorized;
import static com.tychewealth.service.helper.UserTestHelper.retrieveRequest;
import static com.tychewealth.service.helper.UserTestHelper.retrieveRequestUnauthorized;
import static com.tychewealth.service.helper.UserTestHelper.updateRequest;
import static com.tychewealth.service.helper.UserTestHelper.updateRequestUnauthorized;
import static com.tychewealth.testdata.EntityBuilder.buildUser;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tychewealth.config.UserIntegrationTestConfig;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.helper.AuthTokenHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = UserIntegrationTestConfig.class)
@ContextConfiguration(initializers = UserIntegrationTestConfig.Initializer.class)
@AutoConfigureMockMvc
class UserApiControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private AuthTokenHelper authTokenHelper;
  @Autowired private PasswordEncoder passwordEncoder;

  private UserEntity existingUser;

  @BeforeEach
  void setUp() {
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

    updateRequest(mockMvc, accessToken, TEST_UPDATE_USERNAME_REQUEST)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(saved.getId()))
        .andExpect(jsonPath("$.email").value(saved.getEmail()))
        .andExpect(jsonPath("$.username").value(TEST_UPDATE_USERNAME_NORMALIZED));

    UserEntity updatedUser = userRepository.findById(saved.getId()).orElseThrow();
    assertEquals(TEST_UPDATE_USERNAME_NORMALIZED, updatedUser.getUsername());
  }

  @Test
  void updateReturnsUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
    updateRequestUnauthorized(mockMvc, TEST_UPDATE_USERNAME_NORMALIZED)
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.UNAUTHORIZED.getType()))
        .andExpect(jsonPath("$.description").value(ErrorDefinition.UNAUTHORIZED.getDescription()));
  }

  @Test
  void updateReturnsNotFoundWhenAuthenticatedUserWasSoftDeleted() throws Exception {
    UserEntity saved = userRepository.save(existingUser);
    saved.setDeletedAt(java.time.LocalDateTime.now());
    userRepository.save(saved);
    String accessToken = authTokenHelper.generateAccessToken(saved).accessToken();

    updateRequest(mockMvc, accessToken, TEST_UPDATE_USERNAME_NORMALIZED)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.USER_NOT_FOUND.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.USER_NOT_FOUND.getType()))
        .andExpect(
            jsonPath("$.description").value(ErrorDefinition.USER_NOT_FOUND.getDescription()));
  }

  @Test
  void updateReturnsConflictWhenUsernameAlreadyExists() throws Exception {
    UserEntity saved = userRepository.saveAndFlush(existingUser);
    UserEntity anotherUser = new UserEntity();
    anotherUser.setEmail(TEST_OTHER_EMAIL);
    anotherUser.setUsername(TEST_OCCUPIED_USERNAME);
    anotherUser.setPassword(passwordEncoder.encode(TEST_PASSWORD_VALID));
    userRepository.saveAndFlush(anotherUser);
    String accessToken = authTokenHelper.generateAccessToken(saved).accessToken();

    updateRequest(mockMvc, accessToken, TEST_OCCUPIED_USERNAME)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.USER_USERNAME_CONFLICT.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.USER_USERNAME_CONFLICT.getType()))
        .andExpect(
            jsonPath("$.description")
                .value(ErrorDefinition.USER_USERNAME_CONFLICT.getDescription()));
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

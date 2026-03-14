package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.URL_FOLDER_V1;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

  private static final String USER_ME_URL = URL_FOLDER_V1 + "/user/me";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private AuthTokenHelper authTokenHelper;
  @Autowired private PasswordEncoder passwordEncoder;

  private UserEntity existingUser;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();

    existingUser = new UserEntity();
    existingUser.setEmail("laura.gomez@tychewealth.com");
    existingUser.setUsername("lauragomez");
    existingUser.setPassword(passwordEncoder.encode("Secret123!"));
  }

  @Test
  void retrieveReturnsUserWhenUserExists() throws Exception {
    UserEntity saved = userRepository.save(existingUser);
    String accessToken = authTokenHelper.generateAccessToken(saved).accessToken();

    mockMvc
        .perform(get(USER_ME_URL).header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(saved.getId()))
        .andExpect(jsonPath("$.email").value(saved.getEmail()))
        .andExpect(jsonPath("$.username").value(saved.getUsername()))
        .andExpect(jsonPath("$.createdAt").exists())
        .andExpect(jsonPath("$.password").doesNotExist());
  }

  @Test
  void retrieveReturnsUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
    mockMvc
        .perform(get(USER_ME_URL))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.UNAUTHORIZED.getType()))
        .andExpect(jsonPath("$.description").value(ErrorDefinition.UNAUTHORIZED.getDescription()));
  }

  @Test
  void deleteSoftDeletesUserWhenUserIsAuthenticated() throws Exception {
    UserEntity saved = userRepository.save(existingUser);
    String accessToken = authTokenHelper.generateAccessToken(saved).accessToken();

    mockMvc
        .perform(delete(USER_ME_URL).header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isNoContent());

    UserEntity deletedUser = userRepository.findById(saved.getId()).orElseThrow();
    assertNotNull(deletedUser.getDeletedAt());
    assertTrue(userRepository.findByIdAndDeletedAtIsNull(saved.getId()).isEmpty());
  }

  @Test
  void deleteReturnsUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
    mockMvc
        .perform(delete(USER_ME_URL))
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

    mockMvc
        .perform(delete(USER_ME_URL).header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(ErrorDefinition.USER_NOT_FOUND.getCode()))
        .andExpect(jsonPath("$.type").value(ErrorDefinition.USER_NOT_FOUND.getType()))
        .andExpect(
            jsonPath("$.description").value(ErrorDefinition.USER_NOT_FOUND.getDescription()));
  }
}

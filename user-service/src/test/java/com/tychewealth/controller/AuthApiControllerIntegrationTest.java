package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.URL_FOLDER_V1;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.config.IntegrationTestConfig;
import com.tychewealth.dto.user.request.UserCreateRequestDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(IntegrationTestConfig.class)
class AuthApiControllerIntegrationTest {

  private static final String REGISTER_URL = URL_FOLDER_V1 + "/auth/register";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private UserRepository userRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  private UserCreateRequestDto validRequest;
  private UserCreateRequestDto conflictByEmailRequest;
  private UserCreateRequestDto conflictByUsernameRequest;
  private UserEntity existingEmailUser;
  private UserEntity existingUsernameUser;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
    validRequest = new UserCreateRequestDto("john.doe@mail.com", "johndoe", "Secret123");
    conflictByEmailRequest =
        new UserCreateRequestDto(validRequest.getEmail(), "newuser", "Secret123");
    conflictByUsernameRequest =
        new UserCreateRequestDto("new@mail.com", validRequest.getUsername(), "Secret123");

    existingEmailUser = new UserEntity();
    existingEmailUser.setEmail(validRequest.getEmail());
    existingEmailUser.setUsername("otherUser");
    existingEmailUser.setPassword(passwordEncoder.encode("Secret123"));

    existingUsernameUser = new UserEntity();
    existingUsernameUser.setEmail("another@mail.com");
    existingUsernameUser.setUsername(validRequest.getUsername());
    existingUsernameUser.setPassword(passwordEncoder.encode("Secret123"));
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
        .andExpect(jsonPath("$.createdAt").exists());

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
        .andExpect(jsonPath("$.type").value("AUTH_EMAIL_ALREADY_EXISTS_ERROR"))
        .andExpect(
            jsonPath("$.description").value(containsString(conflictByEmailRequest.getEmail())));
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
        .andExpect(jsonPath("$.code").value("TYCHE-101"))
        .andExpect(jsonPath("$.type").value("AUTH_USERNAME_ALREADY_EXISTS_ERROR"))
        .andExpect(
            jsonPath("$.description")
                .value(containsString(conflictByUsernameRequest.getUsername())));
  }

  @ParameterizedTest
  @MethodSource("com.tychewealth.testdata.AuthTestData#invalidCreateRequests")
  void registerReturnsBadRequestForInvalidPayload(
      UserCreateRequestDto invalidRequest, String expectedMessage) throws Exception {
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
}

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
import com.tychewealth.dto.user.request.LoginRequestDto;
import com.tychewealth.dto.user.request.RegisterRequestDto;
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
  private static final String LOGIN_URL = URL_FOLDER_V1 + "/auth/login";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private UserRepository userRepository;

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
        .andExpect(jsonPath("$.type").value("AUTH_LOGIN_CONFLICT"))
        .andExpect(jsonPath("$.description").value("The provided login credentials are invalid"));
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
}

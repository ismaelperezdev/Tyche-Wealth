package com.tychewealth.security.prometheus;

import static com.tychewealth.constants.AuthConstants.AUTHORIZATION_HEADER;
import static com.tychewealth.constants.SecurityConstants.ACTUATOR_PROMETHEUS_PATH;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_INVALID;
import static com.tychewealth.constants.TestConstants.TEST_PROMETHEUS_PASSWORD;
import static com.tychewealth.constants.TestConstants.TEST_PROMETHEUS_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tychewealth.config.security.SecurityIntegrationTestConfig;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = SecurityIntegrationTestConfig.class)
@ContextConfiguration(initializers = SecurityIntegrationTestConfig.Initializer.class)
@AutoConfigureMockMvc
class PrometheusEndpointSecurityIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void rejectsAnonymousAccess() throws Exception {
    mockMvc
        .perform(get(ACTUATOR_PROMETHEUS_PATH).secure(true))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void rejectsInvalidBasicCredentials() throws Exception {
    mockMvc
        .perform(
            get(ACTUATOR_PROMETHEUS_PATH)
                .secure(true)
                .header(
                    AUTHORIZATION_HEADER,
                    basicAuth(TEST_PROMETHEUS_USERNAME, TEST_PASSWORD_INVALID)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void acceptsConfiguredBasicCredentialsOnly() throws Exception {
    mockMvc
        .perform(
            get(ACTUATOR_PROMETHEUS_PATH)
                .secure(true)
                .header(
                    AUTHORIZATION_HEADER,
                    basicAuth(TEST_PROMETHEUS_USERNAME, TEST_PROMETHEUS_PASSWORD)))
        .andExpect(status().isOk());
  }

  private String basicAuth(String username, String password) {
    String credentials = username + ":" + password;
    return "Basic "
        + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
  }
}

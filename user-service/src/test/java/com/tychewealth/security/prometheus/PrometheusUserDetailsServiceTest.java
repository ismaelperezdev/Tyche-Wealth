package com.tychewealth.security.prometheus;

import static com.tychewealth.constants.TestConstants.TEST_MISSING_USERNAME;
import static com.tychewealth.constants.TestConstants.TEST_PROMETHEUS_CREDENTIALS_ERROR;
import static com.tychewealth.constants.TestConstants.TEST_PROMETHEUS_PASSWORD;
import static com.tychewealth.constants.TestConstants.TEST_PROMETHEUS_ROLE;
import static com.tychewealth.constants.TestConstants.TEST_PROMETHEUS_USERNAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tychewealth.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class PrometheusUserDetailsServiceTest {

  private final SecurityConfig securityConfig = new SecurityConfig();
  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @Test
  void throwsWhenPrometheusUsernameIsBlank() {
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                securityConfig.prometheusUserDetailsService(
                    " ", TEST_PROMETHEUS_PASSWORD, passwordEncoder));

    assertEquals(TEST_PROMETHEUS_CREDENTIALS_ERROR, exception.getMessage());
  }

  @Test
  void throwsWhenPrometheusPasswordIsBlank() {
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                securityConfig.prometheusUserDetailsService(
                    TEST_PROMETHEUS_USERNAME, "", passwordEncoder));

    assertEquals(TEST_PROMETHEUS_CREDENTIALS_ERROR, exception.getMessage());
  }

  @Test
  void buildsPrometheusUserWhenCredentialsAreConfigured() {
    UserDetailsService userDetailsService =
        securityConfig.prometheusUserDetailsService(
            TEST_PROMETHEUS_USERNAME, TEST_PROMETHEUS_PASSWORD, passwordEncoder);

    UserDetails user = userDetailsService.loadUserByUsername(TEST_PROMETHEUS_USERNAME);

    assertEquals(TEST_PROMETHEUS_USERNAME, user.getUsername());
    assertTrue(passwordEncoder.matches(TEST_PROMETHEUS_PASSWORD, user.getPassword()));
    assertTrue(
        user.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals(TEST_PROMETHEUS_ROLE)));
    assertThrows(
        UsernameNotFoundException.class,
        () -> userDetailsService.loadUserByUsername(TEST_MISSING_USERNAME));
  }
}

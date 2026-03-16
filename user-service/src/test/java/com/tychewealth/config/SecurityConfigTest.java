package com.tychewealth.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class SecurityConfigTest {

  private final SecurityConfig securityConfig = new SecurityConfig();
  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @Test
  void prometheusUserDetailsServiceThrowsWhenUsernameIsBlank() {
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> securityConfig.prometheusUserDetailsService(" ", "secret", passwordEncoder));

    assertEquals("Prometheus username/password not configured", exception.getMessage());
  }

  @Test
  void prometheusUserDetailsServiceThrowsWhenPasswordIsBlank() {
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> securityConfig.prometheusUserDetailsService("prometheus", "", passwordEncoder));

    assertEquals("Prometheus username/password not configured", exception.getMessage());
  }

  @Test
  void prometheusUserDetailsServiceBuildsPrometheusUserWhenCredentialsConfigured() {
    UserDetailsService userDetailsService =
        securityConfig.prometheusUserDetailsService("prometheus", "secret", passwordEncoder);

    UserDetails user = userDetailsService.loadUserByUsername("prometheus");

    assertEquals("prometheus", user.getUsername());
    assertTrue(passwordEncoder.matches("secret", user.getPassword()));
    assertTrue(
        user.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_PROMETHEUS")));
    assertThrows(
        UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("missing"));
  }
}

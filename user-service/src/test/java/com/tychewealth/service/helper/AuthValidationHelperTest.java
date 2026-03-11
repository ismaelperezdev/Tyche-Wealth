package com.tychewealth.service.helper;

import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_LOGIN_FAILURE;
import static com.tychewealth.constants.AuthConstants.METRIC_AUTH_REGISTER_FAILURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.monitoring.AuthMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthValidationHelperTest {

  private final UserRepository userRepository = mock(UserRepository.class);
  private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final AuthMetrics authMetrics = new AuthMetrics(meterRegistry);
  private final AuthValidationHelper authValidationHelper =
      new AuthValidationHelper(userRepository, passwordEncoder, authMetrics);

  @Test
  void validateRegisterPasswordFormatRecordsRegisterFailure() {
    AuthException exception =
        assertThrows(
            AuthException.class,
            () -> authValidationHelper.validateRegisterPasswordFormat("alllowercase1!"));

    assertEquals(
        ErrorDefinition.AUTH_REGISTER_PASSWORD_FORMAT_INVALID, exception.getErrorDefinition());
    assertEquals(1.0, meterRegistry.get(METRIC_AUTH_REGISTER_FAILURE).counter().count());
    assertEquals(0.0, meterRegistry.get(METRIC_AUTH_LOGIN_FAILURE).counter().count());
  }
}

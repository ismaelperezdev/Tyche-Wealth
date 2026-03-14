package com.tychewealth.testdata;

import com.tychewealth.dto.auth.request.LoginRequestDto;
import com.tychewealth.dto.auth.request.RegisterRequestDto;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public final class AuthTestData {

  private AuthTestData() {}

  public static Stream<Arguments> invalidCreateRequests() {
    return Stream.of(
        Arguments.of(new RegisterRequestDto(" ", "validuser", "Secret123!"), "must not be blank"),
        Arguments.of(
            new RegisterRequestDto("not-an-email", "validuser", "Secret123!"),
            "must be a valid email address"),
        Arguments.of(
            new RegisterRequestDto(buildLongEmail(), "validuser", "Secret123!"),
            "must be at most 254 characters"),
        Arguments.of(
            new RegisterRequestDto("valid@tychewealth.com", " ", "Secret123!"),
            "must not be blank"),
        Arguments.of(
            new RegisterRequestDto("valid@tychewealth.com", "ab", "Secret123!"),
            "must be between 3 and 30 characters"),
        Arguments.of(
            new RegisterRequestDto("valid@tychewealth.com", "a".repeat(31), "Secret123!"),
            "must be between 3 and 30 characters"),
        Arguments.of(
            new RegisterRequestDto("valid@tychewealth.com", "validuser", " "), "must not be blank"),
        Arguments.of(
            new RegisterRequestDto("valid@tychewealth.com", "validuser", "1234567"),
            "must be at least 8 characters"),
        Arguments.of(
            new RegisterRequestDto("valid@tychewealth.com", "validuser", "a".repeat(73)),
            "must be at most 72 bytes when UTF-8 encoded"));
  }

  public static Stream<Arguments> invalidLoginRequests() {
    return Stream.of(
        Arguments.of(new LoginRequestDto(" ", "Secret123!"), "must not be blank"),
        Arguments.of(
            new LoginRequestDto("not-an-email", "Secret123!"), "must be a valid email address"),
        Arguments.of(new LoginRequestDto("valid@tychewealth.com", " "), "must not be blank"),
        Arguments.of(
            new LoginRequestDto("valid@tychewealth.com", "short1!"),
            "must be at least 8 characters"),
        Arguments.of(
            new LoginRequestDto("valid@tychewealth.com", "alllowercase1!"),
            "must include uppercase, lowercase, number and symbol"),
        Arguments.of(
            new LoginRequestDto("valid@tychewealth.com", "a".repeat(73)),
            "must be at most 72 bytes when UTF-8 encoded"));
  }

  private static String buildLongEmail() {
    String local = "a".repeat(64);
    String label63 = "b".repeat(63);
    String domain = label63 + "." + label63 + "." + label63 + ".es";
    return local + "@" + domain;
  }
}

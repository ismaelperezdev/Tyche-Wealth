package com.tychewealth.testdata;

import com.tychewealth.dto.user.request.LoginRequestDto;
import com.tychewealth.dto.user.request.RegisterRequestDto;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public final class AuthTestData {

  private AuthTestData() {}

  public static Stream<Arguments> invalidCreateRequests() {
    return Stream.of(
        Arguments.of(
            new RegisterRequestDto(" ", "validuser", "Secret123"), "Email cannot be blank"),
        Arguments.of(
            new RegisterRequestDto("not-an-email", "validuser", "Secret123"),
            "Email format is invalid"),
        Arguments.of(
            new RegisterRequestDto(buildLongEmail(), "validuser", "Secret123"),
            "Email must be at most 254 characters"),
        Arguments.of(
            new RegisterRequestDto("valid@tychewealth.com", " ", "Secret123"),
            "Username cannot be blank"),
        Arguments.of(
            new RegisterRequestDto("valid@tychewealth.com", "ab", "Secret123"),
            "Username must be between 3 and 30 characters"),
        Arguments.of(
            new RegisterRequestDto("valid@tychewealth.com", "a".repeat(31), "Secret123"),
            "Username must be between 3 and 30 characters"),
        Arguments.of(
            new RegisterRequestDto("valid@tychewealth.com", "validuser", " "),
            "Password cannot be blank"),
        Arguments.of(
            new RegisterRequestDto("valid@tychewealth.com", "validuser", "1234567"),
            "Password must be at least 8 characters"),
        Arguments.of(
            new RegisterRequestDto("valid@tychewealth.com", "validuser", "a".repeat(73)),
            "Password must be at most 72 bytes when UTF-8 encoded"));
  }

  public static Stream<Arguments> invalidLoginRequests() {
    return Stream.of(
        Arguments.of(new LoginRequestDto(" ", "Secret123!"), "Email cannot be blank"),
        Arguments.of(new LoginRequestDto("not-an-email", "Secret123!"), "Email format is invalid"),
        Arguments.of(new LoginRequestDto("valid@tychewealth.com", " "), "Password cannot be blank"),
        Arguments.of(
            new LoginRequestDto("valid@tychewealth.com", "short1!"),
            "Password must be at least 8 characters"),
        Arguments.of(
            new LoginRequestDto("valid@tychewealth.com", "alllowercase1!"),
            "Password must include uppercase, lowercase, number and symbol"),
        Arguments.of(
            new LoginRequestDto("valid@tychewealth.com", "a".repeat(73)),
            "Password must be at most 72 bytes when UTF-8 encoded"));
  }

  private static String buildLongEmail() {
    String domain = "@tychewealth.com";
    int localLength = 254 - domain.length();
    return "a".repeat(localLength) + domain;
  }
}

package com.tychewealth.testdata;

import com.tychewealth.dto.user.request.UserCreateRequestDto;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public final class AuthTestData {

  private AuthTestData() {}

  public static Stream<Arguments> invalidCreateRequests() {
    return Stream.of(
        Arguments.of(
            new UserCreateRequestDto(" ", "validuser", "Secret123"), "Email cannot be blank"),
        Arguments.of(
            new UserCreateRequestDto("not-an-email", "validuser", "Secret123"),
            "Email format is invalid"),
        Arguments.of(
            new UserCreateRequestDto(buildLongEmail(255), "validuser", "Secret123"),
            "Email must be at most 254 characters"),
        Arguments.of(
            new UserCreateRequestDto("valid@mail.com", " ", "Secret123"),
            "Username cannot be blank"),
        Arguments.of(
            new UserCreateRequestDto("valid@mail.com", "ab", "Secret123"),
            "Username must be between 3 and 30 characters"),
        Arguments.of(
            new UserCreateRequestDto("valid@mail.com", "a".repeat(31), "Secret123"),
            "Username must be between 3 and 30 characters"),
        Arguments.of(
            new UserCreateRequestDto("valid@mail.com", "validuser", " "),
            "Password cannot be blank"),
        Arguments.of(
            new UserCreateRequestDto("valid@mail.com", "validuser", "1234567"),
            "Password must be at least 8 characters"),
        Arguments.of(
            new UserCreateRequestDto("valid@mail.com", "validuser", "a".repeat(73)),
            "Password must be at most 72 bytes when UTF-8 encoded"));
  }

  private static String buildLongEmail(int totalLength) {
    String domain = "@mail.com";
    int localLength = totalLength - domain.length();
    return "a".repeat(localLength) + domain;
  }
}

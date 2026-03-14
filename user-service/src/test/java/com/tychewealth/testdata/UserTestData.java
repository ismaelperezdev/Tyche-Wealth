package com.tychewealth.testdata;

import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public final class UserTestData {

  private UserTestData() {}

  public static Stream<Arguments> invalidUpdateRequests() {
    return Stream.of(
        Arguments.of("{\"username\":\" \"}", "must not be blank"),
        Arguments.of("{\"username\":\"ab\"}", "must be between 3 and 30 characters"),
        Arguments.of(
            "{\"username\":\"" + "a".repeat(31) + "\"}", "must be between 3 and 30 characters"));
  }

  public static Stream<Arguments> invalidPasswordUpdateRequests() {
    return Stream.of(
        Arguments.of(
            "{\"currentPassword\":\" \",\"newPassword\":\"NewSecret456!\",\"confirmNewPassword\":\"NewSecret456!\"}",
            "must not be blank"),
        Arguments.of(
            "{\"currentPassword\":\"short1!\",\"newPassword\":\"NewSecret456!\",\"confirmNewPassword\":\"NewSecret456!\"}",
            "must be at least 8 characters"),
        Arguments.of(
            "{\"currentPassword\":\"Secret123!\",\"newPassword\":\" \",\"confirmNewPassword\":\"NewSecret456!\"}",
            "must not be blank"),
        Arguments.of(
            "{\"currentPassword\":\"Secret123!\",\"newPassword\":\"short1!\",\"confirmNewPassword\":\"short1!\"}",
            "must be at least 8 characters"),
        Arguments.of(
            "{\"currentPassword\":\"Secret123!\",\"newPassword\":\"alllowercase1!\",\"confirmNewPassword\":\"alllowercase1!\"}",
            "must include uppercase, lowercase, number and symbol"),
        Arguments.of(
            "{\"currentPassword\":\"Secret123!\",\"newPassword\":\""
                + buildOverBcryptLimitPassword()
                + "\",\"confirmNewPassword\":\""
                + buildOverBcryptLimitPassword()
                + "\"}",
            "must be at most 72 bytes when UTF-8 encoded"),
        Arguments.of(
            "{\"currentPassword\":\"Secret123!\",\"newPassword\":\"NewSecret456!\",\"confirmNewPassword\":\"\"}",
            "must not be blank"),
        Arguments.of(
            "{\"currentPassword\":\"Secret123!\",\"newPassword\":\"NewSecret456!\",\"confirmNewPassword\":\"Mismatch456!\"}",
            "New password and confirm new password must match"));
  }

  private static String buildOverBcryptLimitPassword() {
    return "Aa1!" + "á".repeat(35);
  }
}

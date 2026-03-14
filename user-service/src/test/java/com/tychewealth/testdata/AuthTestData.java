package com.tychewealth.testdata;

import static com.tychewealth.constants.TestConstants.TEST_EMAIL_INVALID;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_VALID;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_LOWERCASE_ONLY;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_TOO_SHORT;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_VALID;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_TOO_SHORT;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_VALID;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_AT_LEAST_8_CHARACTERS;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_AT_MOST_254_CHARACTERS;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_AT_MOST_72_BYTES_WHEN_UTF_8_ENCODED;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_A_VALID_EMAIL_ADDRESS;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_BETWEEN_3_AND_30_CHARACTERS;
import static com.tychewealth.constants.ValidationConstants.MUST_INCLUDE_UPPERCASE_LOWERCASE_NUMBER_AND_SYMBOL;
import static com.tychewealth.constants.ValidationConstants.MUST_NOT_BE_BLANK;
import static com.tychewealth.testhelper.AuthTestHelper.buildLongEmail;

import com.tychewealth.dto.auth.request.LoginRequestDto;
import com.tychewealth.dto.auth.request.RegisterRequestDto;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public final class AuthTestData {

  private AuthTestData() {}

  public static Stream<Arguments> invalidCreateRequests() {
    return Stream.of(
        Arguments.of(
            new RegisterRequestDto(" ", TEST_USERNAME_VALID, TEST_PASSWORD_VALID),
            MUST_NOT_BE_BLANK),
        Arguments.of(
            new RegisterRequestDto(TEST_EMAIL_INVALID, TEST_USERNAME_VALID, TEST_PASSWORD_VALID),
            MUST_BE_A_VALID_EMAIL_ADDRESS),
        Arguments.of(
            new RegisterRequestDto(buildLongEmail(), TEST_USERNAME_VALID, TEST_PASSWORD_VALID),
            MUST_BE_AT_MOST_254_CHARACTERS),
        Arguments.of(
            new RegisterRequestDto(TEST_EMAIL_VALID, " ", TEST_PASSWORD_VALID), MUST_NOT_BE_BLANK),
        Arguments.of(
            new RegisterRequestDto(TEST_EMAIL_VALID, TEST_USERNAME_TOO_SHORT, TEST_PASSWORD_VALID),
            MUST_BE_BETWEEN_3_AND_30_CHARACTERS),
        Arguments.of(
            new RegisterRequestDto(TEST_EMAIL_VALID, "a".repeat(31), TEST_PASSWORD_VALID),
            MUST_BE_BETWEEN_3_AND_30_CHARACTERS),
        Arguments.of(
            new RegisterRequestDto(TEST_EMAIL_VALID, TEST_USERNAME_VALID, " "), MUST_NOT_BE_BLANK),
        Arguments.of(
            new RegisterRequestDto(TEST_EMAIL_VALID, TEST_USERNAME_VALID, "1234567"),
            MUST_BE_AT_LEAST_8_CHARACTERS),
        Arguments.of(
            new RegisterRequestDto(TEST_EMAIL_VALID, TEST_USERNAME_VALID, "a".repeat(73)),
            MUST_BE_AT_MOST_72_BYTES_WHEN_UTF_8_ENCODED));
  }

  public static Stream<Arguments> invalidLoginRequests() {
    return Stream.of(
        Arguments.of(new LoginRequestDto(" ", TEST_PASSWORD_VALID), MUST_NOT_BE_BLANK),
        Arguments.of(
            new LoginRequestDto(TEST_EMAIL_INVALID, TEST_PASSWORD_VALID),
            MUST_BE_A_VALID_EMAIL_ADDRESS),
        Arguments.of(new LoginRequestDto(TEST_EMAIL_VALID, " "), MUST_NOT_BE_BLANK),
        Arguments.of(
            new LoginRequestDto(TEST_EMAIL_VALID, TEST_PASSWORD_TOO_SHORT),
            MUST_BE_AT_LEAST_8_CHARACTERS),
        Arguments.of(
            new LoginRequestDto(TEST_EMAIL_VALID, TEST_PASSWORD_LOWERCASE_ONLY),
            MUST_INCLUDE_UPPERCASE_LOWERCASE_NUMBER_AND_SYMBOL),
        Arguments.of(
            new LoginRequestDto(TEST_EMAIL_VALID, "a".repeat(73)),
            MUST_BE_AT_MOST_72_BYTES_WHEN_UTF_8_ENCODED));
  }
}

package com.tychewealth.testdata;

import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_CONFIRM_MISMATCH;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_LOWERCASE_ONLY;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_NEW_VALID;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_TOO_SHORT;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_VALID;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_TOO_SHORT;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_AT_LEAST_8_CHARACTERS;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_AT_MOST_72_BYTES_WHEN_UTF_8_ENCODED;
import static com.tychewealth.constants.ValidationConstants.MUST_BE_BETWEEN_3_AND_30_CHARACTERS;
import static com.tychewealth.constants.ValidationConstants.MUST_INCLUDE_UPPERCASE_LOWERCASE_NUMBER_AND_SYMBOL;
import static com.tychewealth.constants.ValidationConstants.MUST_NOT_BE_BLANK;
import static com.tychewealth.constants.ValidationConstants.NEW_PASSWORD_AND_CONFIRM_MUST_MATCH;
import static com.tychewealth.testhelper.UserTestHelper.buildOverBcryptLimitPassword;
import static com.tychewealth.testhelper.UserTestHelper.passwordUpdateRequestBody;
import static com.tychewealth.testhelper.UserTestHelper.updateRequestBody;

import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public final class UserTestData {

  private UserTestData() {}

  public static Stream<Arguments> invalidUpdateRequests() {
    return Stream.of(
        Arguments.of(updateRequestBody(" "), MUST_NOT_BE_BLANK),
        Arguments.of(
            updateRequestBody(TEST_USERNAME_TOO_SHORT), MUST_BE_BETWEEN_3_AND_30_CHARACTERS),
        Arguments.of(updateRequestBody("a".repeat(31)), MUST_BE_BETWEEN_3_AND_30_CHARACTERS));
  }

  public static Stream<Arguments> invalidPasswordUpdateRequests() {
    return Stream.of(
        Arguments.of(
            passwordUpdateRequestBody(" ", TEST_PASSWORD_NEW_VALID, TEST_PASSWORD_NEW_VALID),
            MUST_NOT_BE_BLANK),
        Arguments.of(
            passwordUpdateRequestBody(
                TEST_PASSWORD_TOO_SHORT, TEST_PASSWORD_NEW_VALID, TEST_PASSWORD_NEW_VALID),
            MUST_BE_AT_LEAST_8_CHARACTERS),
        Arguments.of(
            passwordUpdateRequestBody(TEST_PASSWORD_VALID, " ", TEST_PASSWORD_NEW_VALID),
            MUST_NOT_BE_BLANK),
        Arguments.of(
            passwordUpdateRequestBody(
                TEST_PASSWORD_VALID, TEST_PASSWORD_TOO_SHORT, TEST_PASSWORD_TOO_SHORT),
            MUST_BE_AT_LEAST_8_CHARACTERS),
        Arguments.of(
            passwordUpdateRequestBody(
                TEST_PASSWORD_VALID, TEST_PASSWORD_LOWERCASE_ONLY, TEST_PASSWORD_LOWERCASE_ONLY),
            MUST_INCLUDE_UPPERCASE_LOWERCASE_NUMBER_AND_SYMBOL),
        Arguments.of(
            passwordUpdateRequestBody(
                TEST_PASSWORD_VALID,
                buildOverBcryptLimitPassword(),
                buildOverBcryptLimitPassword()),
            MUST_BE_AT_MOST_72_BYTES_WHEN_UTF_8_ENCODED),
        Arguments.of(
            passwordUpdateRequestBody(TEST_PASSWORD_VALID, TEST_PASSWORD_NEW_VALID, ""),
            MUST_NOT_BE_BLANK),
        Arguments.of(
            passwordUpdateRequestBody(
                TEST_PASSWORD_VALID, TEST_PASSWORD_NEW_VALID, TEST_PASSWORD_CONFIRM_MISMATCH),
            NEW_PASSWORD_AND_CONFIRM_MUST_MATCH));
  }
}

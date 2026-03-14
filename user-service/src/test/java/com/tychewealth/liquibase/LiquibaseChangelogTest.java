package com.tychewealth.liquibase;

import static com.tychewealth.constants.TestConstants.TEST_EMAIL_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_OCCUPIED_USERNAME;
import static com.tychewealth.constants.TestConstants.TEST_OTHER_EMAIL;
import static com.tychewealth.constants.TestConstants.TEST_REFRESH_TOKEN_EXISTING;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_LAURA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tychewealth.config.UserIntegrationTestConfig;
import com.tychewealth.testhelper.LiquibaseTestHelper;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(classes = UserIntegrationTestConfig.class)
@ContextConfiguration(initializers = UserIntegrationTestConfig.Initializer.class)
@Import(LiquibaseTestHelper.class)
class LiquibaseChangelogTest {

  private static final String CHANGESET_AUTHOR = "tyche-wealth";

  @Autowired private LiquibaseTestHelper liquibaseTestHelper;

  @BeforeEach
  void cleanTables() {
    liquibaseTestHelper.cleanUserRelatedTables();
  }

  @ParameterizedTest
  @MethodSource("indexChangeSetIds")
  void indexChangeSetIsApplied(String changeSetId) {
    assertTrue(liquibaseTestHelper.countAppliedChangeSet(changeSetId, CHANGESET_AUTHOR) > 0);
  }

  @Test
  void usersTableIncludesDeletedAtColumn() {
    assertEquals(1, liquibaseTestHelper.countColumn("USERS", "DELETED_AT"));
  }

  @Test
  void usersEmailMustBeUnique() {
    liquibaseTestHelper.insertUser(1L, TEST_OTHER_EMAIL, TEST_USERNAME_LAURA);

    assertThrows(
        DataIntegrityViolationException.class,
        () -> liquibaseTestHelper.insertUser(2L, TEST_OTHER_EMAIL, TEST_OCCUPIED_USERNAME));
  }

  @Test
  void usersUsernameMustBeUnique() {
    liquibaseTestHelper.insertUser(1L, TEST_EMAIL_LAURA, TEST_OCCUPIED_USERNAME);

    assertThrows(
        DataIntegrityViolationException.class,
        () -> liquibaseTestHelper.insertUser(2L, TEST_OTHER_EMAIL, TEST_OCCUPIED_USERNAME));
  }

  @Test
  void refreshTokenTokenMustBeUnique() {
    liquibaseTestHelper.insertUser(1L, TEST_EMAIL_LAURA, TEST_USERNAME_LAURA);
    liquibaseTestHelper.insertRefreshToken(1L, TEST_REFRESH_TOKEN_EXISTING, 1L);

    assertThrows(
        DataIntegrityViolationException.class,
        () -> liquibaseTestHelper.insertRefreshToken(2L, TEST_REFRESH_TOKEN_EXISTING, 1L));
  }

  @Test
  void refreshTokenMustReferenceExistingUser() {
    assertThrows(
        DataIntegrityViolationException.class,
        () -> liquibaseTestHelper.insertRefreshToken(1L, "orphan-token", 999L));
  }

  private static Stream<String> indexChangeSetIds() {
    return Stream.of("create-users-deleted-at-index", "create-refresh-tokens-user-id-index");
  }
}

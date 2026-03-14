package com.tychewealth.testhelper;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class LiquibaseTestHelper {

  private static final String DEFAULT_STORED_PASSWORD =
      "$2a$10$abcdefghijklmnopqrstuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu";

  private final JdbcTemplate jdbcTemplate;

  LiquibaseTestHelper(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void cleanUserRelatedTables() {
    jdbcTemplate.update("DELETE FROM refresh_tokens");
    jdbcTemplate.update("DELETE FROM users");
  }

  public int countAppliedChangeSet(String changeSetId, String author) {
    Integer appliedCount =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM DATABASECHANGELOG
            WHERE ID = ?
              AND AUTHOR = ?
            """,
            Integer.class,
            changeSetId,
            author);

    return appliedCount == null ? 0 : appliedCount;
  }

  public int countColumn(String tableName, String columnName) {
    Integer columnCount =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_NAME = ?
              AND COLUMN_NAME = ?
            """,
            Integer.class,
            tableName,
            columnName);

    return columnCount == null ? 0 : columnCount;
  }

  public void insertUser(Long id, String email, String username) {
    jdbcTemplate.update(
        """
        INSERT INTO users (id, email, username, password, created_at, deleted_at)
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        id,
        email,
        username,
        DEFAULT_STORED_PASSWORD,
        Timestamp.from(Instant.now()),
        null);
  }

  public void insertRefreshToken(Long id, String token, Long userId) {
    jdbcTemplate.update(
        """
        INSERT INTO refresh_tokens (id, token, user_id, expires_at, revoked, created_at)
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        id,
        token,
        userId,
        Timestamp.from(Instant.now().plusSeconds(3600)),
        false,
        Timestamp.from(Instant.now()));
  }
}

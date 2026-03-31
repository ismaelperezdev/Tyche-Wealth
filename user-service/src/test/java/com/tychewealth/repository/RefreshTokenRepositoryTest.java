package com.tychewealth.repository;

import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_VALID;
import static com.tychewealth.constants.TestConstants.TEST_REFRESH_TOKEN_PEPPER;
import static com.tychewealth.testdata.EntityBuilder.buildRefreshToken;
import static com.tychewealth.testdata.EntityBuilder.buildUser;
import static com.tychewealth.utils.Utils.hmacSha256Hex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tychewealth.entity.RefreshTokenEntity;
import com.tychewealth.entity.UserEntity;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(
    properties = {"spring.liquibase.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class RefreshTokenRepositoryTest {

  private static final String MISSING_TOKEN = "missing-token";
  private static final String SAVED_REFRESH_TOKEN = "refresh-token-123";
  private static final String ANOTHER_SAVED_REFRESH_TOKEN = "refresh-token-456";
  private static final String ACTIVE_TOKEN = "active-token";
  private static final String REVOKED_TOKEN = "revoked-token";
  private static final String OTHER_USER_TOKEN = "other-user-token";
  private static final String ACTIVE_REFRESH_TOKEN = "active-refresh-token";
  private static final String EXPIRED_REFRESH_TOKEN = "expired-refresh-token";
  private static final String REVOKED_REFRESH_TOKEN = "revoked-refresh-token";

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @Autowired private UserRepository userRepository;

  @Test
  void findByTokenReturnsSavedToken() {
    UserEntity user =
        userRepository.save(buildUser("lucia@tyche.com", "lucia", TEST_PASSWORD_VALID));
    Instant expiresAt = Instant.now().plusSeconds(3600);
    refreshTokenRepository.save(buildRefreshToken(SAVED_REFRESH_TOKEN, user, expiresAt, false));

    Optional<RefreshTokenEntity> result =
        refreshTokenRepository.findByToken(
            hmacSha256Hex(SAVED_REFRESH_TOKEN, TEST_REFRESH_TOKEN_PEPPER));

    assertTrue(result.isPresent());
    assertEquals(user.getId(), result.get().getUser().getId());
    assertEquals(expiresAt, result.get().getExpiresAt());
    assertFalse(result.get().isRevoked());
  }

  @Test
  void findByTokenReturnsEmptyWhenTokenDoesNotExist() {
    Optional<RefreshTokenEntity> result =
        refreshTokenRepository.findByToken(hmacSha256Hex(MISSING_TOKEN, TEST_REFRESH_TOKEN_PEPPER));

    assertTrue(result.isEmpty());
  }

  @Test
  void saveAssignsIdAndCreatedAt() {
    UserEntity user =
        userRepository.save(buildUser("marco@tyche.com", "marco", TEST_PASSWORD_VALID));
    RefreshTokenEntity saved =
        refreshTokenRepository.save(
            buildRefreshToken(
                ANOTHER_SAVED_REFRESH_TOKEN, user, Instant.now().plusSeconds(1800), false));

    assertNotNull(saved.getId());
    assertNotNull(saved.getCreatedAt());
  }

  @Test
  void revokeActiveTokensByUserIdRevokesOnlyActiveTokensForSpecifiedUser() {
    UserEntity targetUser =
        userRepository.save(buildUser("sofia@tyche.com", "sofia", TEST_PASSWORD_VALID));
    UserEntity otherUser =
        userRepository.save(buildUser("diego@tyche.com", "diego", TEST_PASSWORD_VALID));
    Instant now = Instant.now();

    refreshTokenRepository.save(
        buildRefreshToken(ACTIVE_TOKEN, targetUser, now.plusSeconds(3600), false));
    refreshTokenRepository.save(
        buildRefreshToken(REVOKED_TOKEN, targetUser, now.plusSeconds(3600), true));
    refreshTokenRepository.save(
        buildRefreshToken(EXPIRED_REFRESH_TOKEN, targetUser, now.minusSeconds(5), false));
    refreshTokenRepository.save(
        buildRefreshToken(OTHER_USER_TOKEN, otherUser, now.plusSeconds(3600), false));

    int revokedCount = refreshTokenRepository.revokeActiveTokensByUserId(targetUser.getId(), now);

    assertEquals(1, revokedCount);
    assertTrue(
        refreshTokenRepository
            .findByToken(hmacSha256Hex(ACTIVE_TOKEN, TEST_REFRESH_TOKEN_PEPPER))
            .orElseThrow()
            .isRevoked());
    assertTrue(
        refreshTokenRepository
            .findByToken(hmacSha256Hex(REVOKED_TOKEN, TEST_REFRESH_TOKEN_PEPPER))
            .orElseThrow()
            .isRevoked());
    assertFalse(
        refreshTokenRepository
            .findByToken(hmacSha256Hex(EXPIRED_REFRESH_TOKEN, TEST_REFRESH_TOKEN_PEPPER))
            .orElseThrow()
            .isRevoked());
    assertFalse(
        refreshTokenRepository
            .findByToken(hmacSha256Hex(OTHER_USER_TOKEN, TEST_REFRESH_TOKEN_PEPPER))
            .orElseThrow()
            .isRevoked());
  }

  @Test
  void revokeTokenIfActiveRevokesOnlyNonExpiredNonRevokedToken() {
    UserEntity user = userRepository.save(buildUser("nora@tyche.com", "nora", TEST_PASSWORD_VALID));
    Instant now = Instant.now();

    refreshTokenRepository.save(
        buildRefreshToken(ACTIVE_REFRESH_TOKEN, user, now.plusSeconds(3600), false));
    refreshTokenRepository.save(
        buildRefreshToken(EXPIRED_REFRESH_TOKEN, user, now.minusSeconds(5), false));
    refreshTokenRepository.save(
        buildRefreshToken(REVOKED_REFRESH_TOKEN, user, now.plusSeconds(3600), true));

    int activeRevoked =
        refreshTokenRepository.revokeTokenIfActive(
            hmacSha256Hex(ACTIVE_REFRESH_TOKEN, TEST_REFRESH_TOKEN_PEPPER), now);
    int expiredRevoked =
        refreshTokenRepository.revokeTokenIfActive(
            hmacSha256Hex(EXPIRED_REFRESH_TOKEN, TEST_REFRESH_TOKEN_PEPPER), now);
    int alreadyRevoked =
        refreshTokenRepository.revokeTokenIfActive(
            hmacSha256Hex(REVOKED_REFRESH_TOKEN, TEST_REFRESH_TOKEN_PEPPER), now);

    assertEquals(1, activeRevoked);
    assertEquals(0, expiredRevoked);
    assertEquals(0, alreadyRevoked);
    assertTrue(
        refreshTokenRepository
            .findByToken(hmacSha256Hex(ACTIVE_REFRESH_TOKEN, TEST_REFRESH_TOKEN_PEPPER))
            .orElseThrow()
            .isRevoked());
    assertFalse(
        refreshTokenRepository
            .findByToken(hmacSha256Hex(EXPIRED_REFRESH_TOKEN, TEST_REFRESH_TOKEN_PEPPER))
            .orElseThrow()
            .isRevoked());
    assertTrue(
        refreshTokenRepository
            .findByToken(hmacSha256Hex(REVOKED_REFRESH_TOKEN, TEST_REFRESH_TOKEN_PEPPER))
            .orElseThrow()
            .isRevoked());
  }
}

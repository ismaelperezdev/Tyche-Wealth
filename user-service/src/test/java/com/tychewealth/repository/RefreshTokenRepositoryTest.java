package com.tychewealth.repository;

import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_VALID;
import static com.tychewealth.testdata.EntityBuilder.buildRefreshToken;
import static com.tychewealth.testdata.EntityBuilder.buildUser;
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

    Optional<RefreshTokenEntity> result = refreshTokenRepository.findByToken(SAVED_REFRESH_TOKEN);

    assertTrue(result.isPresent());
    assertEquals(user.getId(), result.get().getUser().getId());
    assertEquals(expiresAt, result.get().getExpiresAt());
    assertFalse(result.get().isRevoked());
  }

  @Test
  void findByTokenReturnsEmptyWhenTokenDoesNotExist() {
    Optional<RefreshTokenEntity> result = refreshTokenRepository.findByToken(MISSING_TOKEN);

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

    RefreshTokenEntity activeToken =
        refreshTokenRepository.save(
            buildRefreshToken(ACTIVE_TOKEN, targetUser, now.plusSeconds(3600), false));
    RefreshTokenEntity alreadyRevokedToken =
        refreshTokenRepository.save(
            buildRefreshToken(REVOKED_TOKEN, targetUser, now.plusSeconds(3600), true));
    RefreshTokenEntity expiredUnrevokedToken =
        refreshTokenRepository.save(
            buildRefreshToken(EXPIRED_REFRESH_TOKEN, targetUser, now.minusSeconds(5), false));
    RefreshTokenEntity otherUserActiveToken =
        refreshTokenRepository.save(
            buildRefreshToken(OTHER_USER_TOKEN, otherUser, now.plusSeconds(3600), false));

    int revokedCount = refreshTokenRepository.revokeActiveTokensByUserId(targetUser.getId(), now);

    assertEquals(1, revokedCount);
    assertTrue(
        refreshTokenRepository.findByToken(activeToken.getToken()).orElseThrow().isRevoked());
    assertTrue(
        refreshTokenRepository
            .findByToken(alreadyRevokedToken.getToken())
            .orElseThrow()
            .isRevoked());
    assertFalse(
        refreshTokenRepository
            .findByToken(expiredUnrevokedToken.getToken())
            .orElseThrow()
            .isRevoked());
    assertFalse(
        refreshTokenRepository
            .findByToken(otherUserActiveToken.getToken())
            .orElseThrow()
            .isRevoked());
  }

  @Test
  void revokeTokenIfActiveRevokesOnlyNonExpiredNonRevokedToken() {
    UserEntity user = userRepository.save(buildUser("nora@tyche.com", "nora", TEST_PASSWORD_VALID));
    Instant now = Instant.now();

    RefreshTokenEntity activeToken =
        refreshTokenRepository.save(
            buildRefreshToken(ACTIVE_REFRESH_TOKEN, user, now.plusSeconds(3600), false));
    refreshTokenRepository.save(
        buildRefreshToken(EXPIRED_REFRESH_TOKEN, user, now.minusSeconds(5), false));
    refreshTokenRepository.save(
        buildRefreshToken(REVOKED_REFRESH_TOKEN, user, now.plusSeconds(3600), true));

    int activeRevoked = refreshTokenRepository.revokeTokenIfActive(activeToken.getToken(), now);
    int expiredRevoked = refreshTokenRepository.revokeTokenIfActive(EXPIRED_REFRESH_TOKEN, now);
    int alreadyRevoked = refreshTokenRepository.revokeTokenIfActive(REVOKED_REFRESH_TOKEN, now);

    assertEquals(1, activeRevoked);
    assertEquals(0, expiredRevoked);
    assertEquals(0, alreadyRevoked);
    assertTrue(
        refreshTokenRepository.findByToken(activeToken.getToken()).orElseThrow().isRevoked());
    assertFalse(
        refreshTokenRepository.findByToken(EXPIRED_REFRESH_TOKEN).orElseThrow().isRevoked());
    assertTrue(refreshTokenRepository.findByToken(REVOKED_REFRESH_TOKEN).orElseThrow().isRevoked());
  }
}

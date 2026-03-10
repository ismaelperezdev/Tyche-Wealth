package com.tychewealth.repository;

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

  @Autowired private RefreshTokenRepository refreshTokenRepository;

  @Autowired private UserRepository userRepository;

  @Test
  void findByTokenReturnsSavedToken() {
    UserEntity user = userRepository.save(buildUser("lucia@tyche.com", "lucia"));
    Instant expiresAt = Instant.now().plusSeconds(3600);
    refreshTokenRepository.save(buildRefreshToken("refresh-token-123", user, expiresAt, false));

    Optional<RefreshTokenEntity> result = refreshTokenRepository.findByToken("refresh-token-123");

    assertTrue(result.isPresent());
    assertEquals(user.getId(), result.get().getUser().getId());
    assertEquals(expiresAt, result.get().getExpiresAt());
    assertFalse(result.get().isRevoked());
  }

  @Test
  void findByTokenReturnsEmptyWhenTokenDoesNotExist() {
    Optional<RefreshTokenEntity> result = refreshTokenRepository.findByToken("missing-token");

    assertTrue(result.isEmpty());
  }

  @Test
  void saveAssignsIdAndCreatedAt() {
    UserEntity user = userRepository.save(buildUser("marco@tyche.com", "marco"));
    RefreshTokenEntity saved =
        refreshTokenRepository.save(
            buildRefreshToken("refresh-token-456", user, Instant.now().plusSeconds(1800), false));

    assertNotNull(saved.getId());
    assertNotNull(saved.getCreatedAt());
  }
}

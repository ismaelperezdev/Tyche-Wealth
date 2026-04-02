package com.tychewealth.repository;

import static com.tychewealth.constants.TestConstants.TEST_EMAIL_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_VALID;
import static com.tychewealth.constants.TestConstants.TEST_OCCUPIED_USERNAME;
import static com.tychewealth.constants.TestConstants.TEST_OTHER_EMAIL;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_VALID;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_VALID;
import static com.tychewealth.testdata.EntityBuilder.buildTrustedDevice;
import static com.tychewealth.testdata.EntityBuilder.buildUser;
import static com.tychewealth.utils.Utils.sha256Hex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tychewealth.entity.TrustedDeviceEntity;
import com.tychewealth.entity.UserEntity;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest(
    properties = {"spring.liquibase.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class TrustedDeviceRepositoryTest {

  private static final String ACTIVE_TOKEN = "active-trusted-device-token";
  private static final String EXPIRED_TOKEN = "expired-trusted-device-token";
  private static final String OTHER_USER_TOKEN = "other-user-trusted-device-token";
  private static final String MISSING_TOKEN = "missing-trusted-device-token";

  @Autowired private TrustedDeviceRepository trustedDeviceRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private TestEntityManager testEntityManager;

  @Test
  void countByUserIdAndExpiresAtAfterCountsOnlyActiveDevicesForSpecifiedUser() {
    UserEntity targetUser =
        userRepository.save(buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_PASSWORD_VALID));
    UserEntity otherUser =
        userRepository.save(
            buildUser(TEST_OTHER_EMAIL, TEST_OCCUPIED_USERNAME, TEST_PASSWORD_VALID));
    Instant now = Instant.now();

    trustedDeviceRepository.save(
        buildTrustedDevice(ACTIVE_TOKEN, targetUser, now.plusSeconds(3600), now));
    trustedDeviceRepository.save(
        buildTrustedDevice(EXPIRED_TOKEN, targetUser, now.minusSeconds(5), now.minusSeconds(10)));
    trustedDeviceRepository.save(
        buildTrustedDevice(OTHER_USER_TOKEN, otherUser, now.plusSeconds(3600), now));

    long result = trustedDeviceRepository.countByUserIdAndExpiresAtAfter(targetUser.getId(), now);

    assertEquals(1L, result);
  }

  @Test
  void deleteByUserIdAndExpiresAtBeforeDeletesOnlyExpiredDevicesForSpecifiedUser() {
    UserEntity targetUser =
        userRepository.save(buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_PASSWORD_VALID));
    UserEntity otherUser =
        userRepository.save(
            buildUser(TEST_OTHER_EMAIL, TEST_OCCUPIED_USERNAME, TEST_PASSWORD_VALID));
    Instant now = Instant.now();

    TrustedDeviceEntity expiredTrustedDevice =
        trustedDeviceRepository.save(
            buildTrustedDevice(
                EXPIRED_TOKEN, targetUser, now.minusSeconds(5), now.minusSeconds(10)));
    TrustedDeviceEntity activeTrustedDevice =
        trustedDeviceRepository.save(
            buildTrustedDevice(ACTIVE_TOKEN, targetUser, now.plusSeconds(3600), now));
    TrustedDeviceEntity otherUserTrustedDevice =
        trustedDeviceRepository.save(
            buildTrustedDevice(
                OTHER_USER_TOKEN, otherUser, now.minusSeconds(5), now.minusSeconds(10)));

    trustedDeviceRepository.deleteByUserIdAndExpiresAtBefore(targetUser.getId(), now);
    testEntityManager.flush();
    testEntityManager.clear();

    assertFalse(trustedDeviceRepository.findById(expiredTrustedDevice.getId()).isPresent());
    assertTrue(trustedDeviceRepository.findById(activeTrustedDevice.getId()).isPresent());
    assertTrue(trustedDeviceRepository.findById(otherUserTrustedDevice.getId()).isPresent());
  }

  @Test
  void findByUserIdAndTokenHashAndExpiresAtAfterReturnsActiveTrustedDevice() {
    UserEntity user =
        userRepository.save(buildUser(TEST_EMAIL_VALID, TEST_USERNAME_VALID, TEST_PASSWORD_VALID));
    Instant expiresAt = Instant.now().plusSeconds(3600);
    trustedDeviceRepository.save(buildTrustedDevice(ACTIVE_TOKEN, user, expiresAt, Instant.now()));

    Optional<TrustedDeviceEntity> result =
        trustedDeviceRepository.findByUserIdAndTokenHashAndExpiresAtAfter(
            user.getId(), sha256Hex(ACTIVE_TOKEN), Instant.now());

    assertTrue(result.isPresent());
    assertEquals(user.getId(), result.get().getUser().getId());
    assertEquals(expiresAt, result.get().getExpiresAt());
  }

  @Test
  void findByUserIdAndTokenHashAndExpiresAtAfterReturnsEmptyForMissingOrExpiredDevice() {
    UserEntity user =
        userRepository.save(buildUser(TEST_EMAIL_VALID, TEST_USERNAME_VALID, TEST_PASSWORD_VALID));
    Instant now = Instant.now();
    trustedDeviceRepository.save(
        buildTrustedDevice(EXPIRED_TOKEN, user, now.minusSeconds(5), now.minusSeconds(10)));

    Optional<TrustedDeviceEntity> missingResult =
        trustedDeviceRepository.findByUserIdAndTokenHashAndExpiresAtAfter(
            user.getId(), sha256Hex(MISSING_TOKEN), now);
    Optional<TrustedDeviceEntity> expiredResult =
        trustedDeviceRepository.findByUserIdAndTokenHashAndExpiresAtAfter(
            user.getId(), sha256Hex(EXPIRED_TOKEN), now);

    assertTrue(missingResult.isEmpty());
    assertTrue(expiredResult.isEmpty());
  }

  @Test
  void saveAssignsIdAndCreatedAt() {
    UserEntity user =
        userRepository.save(buildUser(TEST_EMAIL_VALID, TEST_USERNAME_VALID, TEST_PASSWORD_VALID));

    TrustedDeviceEntity saved =
        trustedDeviceRepository.save(
            buildTrustedDevice(ACTIVE_TOKEN, user, Instant.now().plusSeconds(3600), Instant.now()));

    assertNotNull(saved.getId());
    assertNotNull(saved.getCreatedAt());
  }
}

package com.tychewealth.repository;

import static com.tychewealth.constants.TestConstants.TEST_EMAIL_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_OCCUPIED_USERNAME;
import static com.tychewealth.constants.TestConstants.TEST_OTHER_EMAIL;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_VALID;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_LAURA;
import static com.tychewealth.testdata.EntityBuilder.buildUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tychewealth.entity.UserEntity;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(
    properties = {"spring.liquibase.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class UserRepositoryTest {

  @Autowired private UserRepository userRepository;

  @Test
  void findByEmailReturnsSavedUser() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_PASSWORD_VALID);
    userRepository.save(user);

    Optional<UserEntity> result = userRepository.findByEmailIncludingDeleted(TEST_EMAIL_LAURA);

    assertTrue(result.isPresent());
    assertEquals(TEST_USERNAME_LAURA, result.get().getUsername());
  }

  @Test
  void findByUsernameReturnsSavedUser() {
    UserEntity user = buildUser(TEST_OTHER_EMAIL, TEST_OCCUPIED_USERNAME, TEST_PASSWORD_VALID);
    userRepository.save(user);

    Optional<UserEntity> result =
        userRepository.findByUsernameIncludingDeleted(TEST_OCCUPIED_USERNAME);

    assertTrue(result.isPresent());
    assertEquals(TEST_OTHER_EMAIL, result.get().getEmail());
  }

  @Test
  void findByIdAndDeletedAtIsNullExcludesSoftDeletedUser() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, TEST_PASSWORD_VALID);
    user.setDeletedAt(LocalDateTime.now());
    UserEntity saved = userRepository.save(user);

    Optional<UserEntity> result = userRepository.findByIdAndDeletedAtIsNull(saved.getId());

    assertTrue(result.isEmpty());
  }
}

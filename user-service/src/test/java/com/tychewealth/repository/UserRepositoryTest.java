package com.tychewealth.repository;

import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_VALID;
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
    UserEntity user = buildUser("maria@tyche.com", "maria", TEST_PASSWORD_VALID);
    userRepository.save(user);

    Optional<UserEntity> result = userRepository.findByEmailIncludingDeleted("maria@tyche.com");

    assertTrue(result.isPresent());
    assertEquals("maria", result.get().getUsername());
  }

  @Test
  void findByUsernameReturnsSavedUser() {
    UserEntity user = buildUser("carlos@tyche.com", "carlos", TEST_PASSWORD_VALID);
    userRepository.save(user);

    Optional<UserEntity> result = userRepository.findByUsernameIncludingDeleted("carlos");

    assertTrue(result.isPresent());
    assertEquals("carlos@tyche.com", result.get().getEmail());
  }

  @Test
  void findByIdAndDeletedAtIsNullExcludesSoftDeletedUser() {
    UserEntity user = buildUser("lucia@tyche.com", "lucia", TEST_PASSWORD_VALID);
    user.setDeletedAt(LocalDateTime.now());
    UserEntity saved = userRepository.save(user);

    Optional<UserEntity> result = userRepository.findByIdAndDeletedAtIsNull(saved.getId());

    assertTrue(result.isEmpty());
  }
}

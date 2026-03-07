package com.tychewealth.repository;

import com.tychewealth.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmailReturnsSavedUser() {
        UserEntity user = buildUser("maria@tyche.com", "maria");
        userRepository.save(user);

        Optional<UserEntity> result = userRepository.findByEmail("maria@tyche.com");

        assertTrue(result.isPresent());
        assertEquals("maria", result.get().getUsername());
    }

    @Test
    void findByUsernameReturnsSavedUser() {
        UserEntity user = buildUser("carlos@tyche.com", "carlos");
        userRepository.save(user);

        Optional<UserEntity> result = userRepository.findByUsername("carlos");

        assertTrue(result.isPresent());
        assertEquals("carlos@tyche.com", result.get().getEmail());
    }

    private UserEntity buildUser(String email, String username) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword("secret");
        return user;
    }
}

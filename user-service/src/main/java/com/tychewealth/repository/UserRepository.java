package com.tychewealth.repository;

import com.tychewealth.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

  Optional<UserEntity> findByEmail(String email);

  Optional<UserEntity> findByUsername(String username);

  Optional<UserEntity> findByIdAndDeletedAtIsNull(Long id);

  Optional<UserEntity> findByEmailAndDeletedAtIsNull(String email);

  Optional<UserEntity> findByUsernameAndDeletedAtIsNull(String username);
}

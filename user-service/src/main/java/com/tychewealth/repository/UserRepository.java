package com.tychewealth.repository;

import com.tychewealth.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

  @Query("SELECT u FROM UserEntity u WHERE u.email = :email")
  Optional<UserEntity> findByEmailIncludingDeleted(@Param("email") String email);

  @Query("SELECT u FROM UserEntity u WHERE u.username = :username")
  Optional<UserEntity> findByUsernameIncludingDeleted(@Param("username") String username);

  Optional<UserEntity> findByIdAndDeletedAtIsNull(Long id);

  Optional<UserEntity> findByEmailAndDeletedAtIsNull(String email);

  Optional<UserEntity> findByUsernameAndDeletedAtIsNull(String username);
}

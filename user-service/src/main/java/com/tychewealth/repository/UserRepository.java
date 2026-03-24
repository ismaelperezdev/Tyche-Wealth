package com.tychewealth.repository;

import com.tychewealth.entity.UserEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM UserEntity u WHERE u.id = :id")
  Optional<UserEntity> findByIdForUpdate(@Param("id") Long id);

  Optional<UserEntity> findByEmailAndDeletedAtIsNull(String email);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM UserEntity u WHERE u.email = :email AND u.deletedAt IS NULL")
  Optional<UserEntity> findByEmailAndDeletedAtIsNullForUpdate(@Param("email") String email);

  Optional<UserEntity> findByUsernameAndDeletedAtIsNull(String username);
}

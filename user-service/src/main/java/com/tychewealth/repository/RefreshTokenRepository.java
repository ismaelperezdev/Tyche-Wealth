package com.tychewealth.repository;

import com.tychewealth.entity.RefreshTokenEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

  Optional<RefreshTokenEntity> findByToken(String token);

  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update RefreshTokenEntity rt
         set rt.revoked = true
       where rt.user.id = :userId
         and rt.revoked = false
         and rt.expiresAt > :currentTime
      """)
  int revokeActiveTokensByUserId(
      @Param("userId") Long userId, @Param("currentTime") Instant currentTime);

  @Transactional
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update RefreshTokenEntity rt
         set rt.revoked = true
       where rt.token = :token
         and rt.revoked = false
         and rt.expiresAt > :currentTime
      """)
  int revokeTokenIfActive(@Param("token") String token, @Param("currentTime") Instant currentTime);
}

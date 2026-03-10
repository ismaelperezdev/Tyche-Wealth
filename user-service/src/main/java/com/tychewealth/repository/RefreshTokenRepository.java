package com.tychewealth.repository;

import com.tychewealth.entity.RefreshTokenEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

  Optional<RefreshTokenEntity> findByToken(String token);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update RefreshTokenEntity rt set rt.revoked = true where rt.user.id = :userId and rt.revoked = false")
  int revokeActiveTokensByUserId(@Param("userId") Long userId);
}

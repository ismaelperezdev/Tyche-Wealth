package com.tychewealth.repository;

import com.tychewealth.entity.TrustedDeviceEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrustedDeviceRepository extends JpaRepository<TrustedDeviceEntity, Long> {

  long countByUserIdAndExpiresAtAfter(Long userId, Instant expiresAt);

  void deleteByUserIdAndExpiresAtBefore(Long userId, Instant expiresAt);

  Optional<TrustedDeviceEntity> findByUserIdAndTokenHashAndExpiresAtAfter(
      Long userId, String tokenHash, Instant expiresAt);
}

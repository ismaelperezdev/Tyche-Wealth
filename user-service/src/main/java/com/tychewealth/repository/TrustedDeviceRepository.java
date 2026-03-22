package com.tychewealth.repository;

import com.tychewealth.entity.TrustedDeviceEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrustedDeviceRepository extends JpaRepository<TrustedDeviceEntity, Long> {

  long countByUserId(Long userId);

  Optional<TrustedDeviceEntity> findByTokenHash(String tokenHash);

  List<TrustedDeviceEntity> findByUserIdOrderByCreatedAtAsc(Long userId);
}

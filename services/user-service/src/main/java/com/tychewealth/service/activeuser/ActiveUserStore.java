package com.tychewealth.service.activeuser;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public interface ActiveUserStore {

  void replaceAll(Set<Long> userIds);

  Set<Long> findAll();

  void updateLastRefresh(Instant refreshedAt);

  Optional<Instant> findLastRefresh();
}

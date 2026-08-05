package com.tychewealth.service.activeuser;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * Storage abstraction for the distributed snapshot of users with active refresh tokens.
 *
 * <p>Allows the snapshot service to replace the complete set of active user identifiers and track
 * when that snapshot was last refreshed without coupling the use case to a specific backing store.
 */
public interface ActiveUserStore {

  void replaceAll(Set<Long> userIds);

  Set<Long> findAll();

  void updateLastRefresh(Instant refreshedAt);

  Optional<Instant> findLastRefresh();
}

package com.tychewealth.service.activeuser;

import com.tychewealth.repository.RefreshTokenRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Rebuilds the active-user snapshot from currently valid refresh tokens.
 *
 * <p>Queries distinct users with non-expired, non-revoked refresh tokens, replaces the distributed
 * store contents, records the refresh timestamp, and returns an immutable snapshot for event
 * publication by the scheduler.
 */
@Service
@RequiredArgsConstructor
public class ActiveUserSnapshotService {

  private final RefreshTokenRepository refreshTokenRepository;
  private final ActiveUserStore activeUserStore;

  public Set<Long> refreshSnapshot() {
    Instant refreshedAt = Instant.now();
    Set<Long> userIds =
        new LinkedHashSet<>(
            refreshTokenRepository.findDistinctUserIdsWithActiveTokens(refreshedAt));
    activeUserStore.replaceAll(userIds);
    activeUserStore.updateLastRefresh(refreshedAt);
    return Set.copyOf(userIds);
  }
}

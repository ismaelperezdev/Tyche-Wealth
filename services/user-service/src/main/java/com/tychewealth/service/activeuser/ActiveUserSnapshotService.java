package com.tychewealth.service.activeuser;

import com.tychewealth.repository.RefreshTokenRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

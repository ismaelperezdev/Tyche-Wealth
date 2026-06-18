package com.tychewealth.service.activeuser;

import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.repository.RefreshTokenRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActiveUserSnapshotServiceTest {

  private static final Long TEST_SECOND_USER_ID = 7L;

  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private ActiveUserStore activeUserStore;

  private ActiveUserSnapshotService activeUserSnapshotService;

  @BeforeEach
  void setUp() {
    activeUserSnapshotService =
        new ActiveUserSnapshotService(refreshTokenRepository, activeUserStore);
  }

  @Test
  void refreshSnapshotRebuildsActiveUsersUpdatesRefreshTimestampAndReturnsDistinctIds() {
    when(refreshTokenRepository.findDistinctUserIdsWithActiveTokens(any(Instant.class)))
        .thenReturn(List.of(TEST_USER_ID, TEST_SECOND_USER_ID, TEST_USER_ID));
    ArgumentCaptor<Set<Long>> userIdsCaptor = ArgumentCaptor.forClass(Set.class);
    ArgumentCaptor<Instant> refreshedAtCaptor = ArgumentCaptor.forClass(Instant.class);

    Set<Long> result = activeUserSnapshotService.refreshSnapshot();

    verify(refreshTokenRepository).findDistinctUserIdsWithActiveTokens(any(Instant.class));
    verify(activeUserStore).replaceAll(userIdsCaptor.capture());
    verify(activeUserStore).updateLastRefresh(refreshedAtCaptor.capture());
    assertEquals(Set.of(TEST_USER_ID, TEST_SECOND_USER_ID), result);
    assertEquals(Set.of(TEST_USER_ID, TEST_SECOND_USER_ID), userIdsCaptor.getValue());
    assertNotNull(refreshedAtCaptor.getValue());
  }
}

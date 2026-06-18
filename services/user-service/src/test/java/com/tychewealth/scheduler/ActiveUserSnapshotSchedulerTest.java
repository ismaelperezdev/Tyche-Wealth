package com.tychewealth.scheduler;

import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.service.activeuser.ActiveUserSnapshotService;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActiveUserSnapshotSchedulerTest {

  @Mock private ActiveUserSnapshotService activeUserSnapshotService;

  private ActiveUserSnapshotScheduler activeUserSnapshotScheduler;

  @BeforeEach
  void setUp() {
    activeUserSnapshotScheduler = new ActiveUserSnapshotScheduler(activeUserSnapshotService);
  }

  @Test
  void refreshActiveUsersSnapshotDelegatesToSnapshotService() {
    when(activeUserSnapshotService.refreshSnapshot()).thenReturn(Set.of(TEST_USER_ID));

    activeUserSnapshotScheduler.refreshActiveUsersSnapshot();

    verify(activeUserSnapshotService).refreshSnapshot();
  }

  @Test
  void refreshActiveUsersSnapshotSwallowsRuntimeExceptionsFromSnapshotService() {
    when(activeUserSnapshotService.refreshSnapshot())
        .thenThrow(new IllegalStateException("snapshot failed"));

    assertDoesNotThrow(() -> activeUserSnapshotScheduler.refreshActiveUsersSnapshot());

    verify(activeUserSnapshotService).refreshSnapshot();
  }
}

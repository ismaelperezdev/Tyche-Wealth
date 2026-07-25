package com.tychewealth.scheduler;

import static com.tychewealth.constants.TestConstants.TEST_USER_ID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.kafka.events.ActiveUsersEvent;
import com.tychewealth.kafka.publishers.ActiveUsersEventPublisher;
import com.tychewealth.service.activeuser.ActiveUserSnapshotService;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActiveUserSnapshotSchedulerTest {

  @Mock private ActiveUserSnapshotService activeUserSnapshotService;
  @Mock private ActiveUsersEventPublisher activeUsersEventPublisher;

  private ActiveUserSnapshotScheduler activeUserSnapshotScheduler;

  @BeforeEach
  void setUp() {
    activeUserSnapshotScheduler =
        new ActiveUserSnapshotScheduler(activeUserSnapshotService, activeUsersEventPublisher);
  }

  @Test
  void refreshActiveUsersSnapshotDelegatesToSnapshotServiceAndPublishesEvent() {
    Set<Long> userIds = Set.of(TEST_USER_ID);
    when(activeUserSnapshotService.refreshSnapshot()).thenReturn(userIds);

    activeUserSnapshotScheduler.refreshActiveUsersSnapshot();

    ArgumentCaptor<ActiveUsersEvent> eventCaptor = ArgumentCaptor.forClass(ActiveUsersEvent.class);

    verify(activeUserSnapshotService).refreshSnapshot();
    verify(activeUsersEventPublisher).publish(eventCaptor.capture());
    assertEquals(userIds, eventCaptor.getValue().userIds());
    assertNotNull(eventCaptor.getValue().occurredAt());
  }

  @Test
  void refreshActiveUsersSnapshotSwallowsRuntimeExceptionsFromSnapshotService() {
    when(activeUserSnapshotService.refreshSnapshot())
        .thenThrow(new IllegalStateException("snapshot failed"));

    assertDoesNotThrow(() -> activeUserSnapshotScheduler.refreshActiveUsersSnapshot());

    verify(activeUserSnapshotService).refreshSnapshot();
    verify(activeUsersEventPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
  }
}

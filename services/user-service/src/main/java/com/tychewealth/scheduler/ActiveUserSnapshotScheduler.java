package com.tychewealth.scheduler;

import static com.tychewealth.constants.LogConstants.ACTIVE_USER_SNAPSHOT_ACTION;
import static com.tychewealth.constants.LogConstants.ACTIVE_USER_SNAPSHOT_FAILURE_MESSAGE;
import static com.tychewealth.constants.LogConstants.ACTIVE_USER_SNAPSHOT_SUCCESS_CONTEXT;
import static com.tychewealth.constants.LogConstants.REQUEST_CONFLICT;
import static com.tychewealth.constants.LogConstants.REQUEST_START;
import static com.tychewealth.constants.LogConstants.REQUEST_SUCCESS;
import static com.tychewealth.constants.LogConstants.SYSTEM;

import com.tychewealth.kafka.events.ActiveUsersEvent;
import com.tychewealth.kafka.publishers.ActiveUsersEventPublisher;
import com.tychewealth.service.activeuser.ActiveUserSnapshotService;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    value = "app.active-users.snapshot.enabled",
    havingValue = "true",
    matchIfMissing = true)
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = false)
public class ActiveUserSnapshotScheduler {

  private final ActiveUserSnapshotService activeUserSnapshotService;
  private final ActiveUsersEventPublisher activeUsersEventPublisher;

  @Scheduled(
      fixedDelayString = "${app.active-users.snapshot.fixed-delay:5m}",
      initialDelayString = "${app.active-users.snapshot.initial-delay:0s}")
  public void refreshActiveUsersSnapshot() {
    long start = System.currentTimeMillis();
    log.info(REQUEST_START, SYSTEM, ACTIVE_USER_SNAPSHOT_ACTION);

    try {
      Set<Long> userIds = activeUserSnapshotService.refreshSnapshot();
      activeUsersEventPublisher.publish(new ActiveUsersEvent(Instant.now(), userIds));
      int activeUsers = userIds.size();
      log.info(
          REQUEST_SUCCESS + ACTIVE_USER_SNAPSHOT_SUCCESS_CONTEXT,
          SYSTEM,
          ACTIVE_USER_SNAPSHOT_ACTION,
          activeUsers,
          System.currentTimeMillis() - start);
      log.debug(
          REQUEST_SUCCESS + ACTIVE_USER_SNAPSHOT_SUCCESS_CONTEXT,
          SYSTEM,
          ACTIVE_USER_SNAPSHOT_ACTION,
          activeUsers,
          System.currentTimeMillis() - start);
    } catch (RuntimeException error) {
      log.error(
          REQUEST_CONFLICT,
          SYSTEM,
          ACTIVE_USER_SNAPSHOT_ACTION,
          ACTIVE_USER_SNAPSHOT_FAILURE_MESSAGE,
          error);
    }
  }
}

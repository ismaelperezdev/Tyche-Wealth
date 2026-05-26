package com.tychewealth.scheduler;

import static com.tychewealth.constants.LogConstants.POLLING_DURATION;
import static com.tychewealth.constants.LogConstants.POLLING_FAILURE;
import static com.tychewealth.constants.LogConstants.POLLING_NO_CHANGES;
import static com.tychewealth.constants.LogConstants.POLLING_SUCCESS;
import static com.tychewealth.constants.LogConstants.POLL_ACTION;
import static com.tychewealth.constants.LogConstants.VEHICLE_POLLING_SCHEDULER;

import com.tychewealth.service.VehiclePollingService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    value = "vehicle.polling.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class VehiclePollingScheduler {

  private final VehiclePollingService vehiclePollingService;

  @Scheduled(
      fixedDelayString = "${vehicle.polling.fixed-delay}",
      initialDelayString = "${vehicle.polling.initial-delay}")
  public void pollVehicles() {
    long start = System.currentTimeMillis();

    try {
      var pollingResult =
          Objects.requireNonNull(
              vehiclePollingService.pollVehicles().block(), "Vehicle polling returned no result");

      if (pollingResult.hasChanges()) {
        log.info(
            POLLING_SUCCESS,
            VEHICLE_POLLING_SCHEDULER,
            POLL_ACTION,
            pollingResult.addedVehicles().size(),
            pollingResult.removedVehicles().size());
      } else {
        log.info(POLLING_NO_CHANGES, VEHICLE_POLLING_SCHEDULER, POLL_ACTION);
      }

      log.debug(
          POLLING_DURATION,
          VEHICLE_POLLING_SCHEDULER,
          POLL_ACTION,
          System.currentTimeMillis() - start);
    } catch (RuntimeException error) {
      log.error(POLLING_FAILURE, VEHICLE_POLLING_SCHEDULER, POLL_ACTION, error);
    }
  }
}

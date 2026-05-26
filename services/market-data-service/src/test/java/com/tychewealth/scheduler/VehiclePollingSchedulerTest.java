package com.tychewealth.scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.model.VehicleChanges;
import com.tychewealth.service.VehiclePollingService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class VehiclePollingSchedulerTest {

  @Mock private VehiclePollingService vehiclePollingService;

  private VehiclePollingScheduler vehiclePollingScheduler;

  @BeforeEach
  void setUp() {
    vehiclePollingScheduler = new VehiclePollingScheduler(vehiclePollingService);
  }

  @Test
  void shouldTriggerVehiclePolling() {
    when(vehiclePollingService.pollVehicles())
        .thenReturn(Mono.just(new VehicleChanges(List.of(), List.of())));

    vehiclePollingScheduler.pollVehicles();

    verify(vehiclePollingService).pollVehicles();
  }

  @Test
  void shouldHandlePollingError() {
    when(vehiclePollingService.pollVehicles())
        .thenReturn(Mono.error(new RuntimeException("Polling failed")));

    vehiclePollingScheduler.pollVehicles();

    verify(vehiclePollingService).pollVehicles();
  }
}

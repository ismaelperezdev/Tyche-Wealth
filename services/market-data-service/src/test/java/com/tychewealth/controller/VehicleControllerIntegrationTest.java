package com.tychewealth.controller;

import static com.tychewealth.constants.TestConstants.FIRST_VEHICLE_ID;
import static com.tychewealth.constants.TestConstants.SECOND_VEHICLE_ID;
import static com.tychewealth.constants.TestConstants.VEHICLES_ENDPOINT;
import static com.tychewealth.error.ErrorDefinition.GENERIC_INTERNAL_ERROR;
import static com.tychewealth.testdata.ClientTestData.VEHICLE_ID;
import static com.tychewealth.testdata.ClientTestData.defaultExternalVehicleDto;
import static com.tychewealth.testdata.ClientTestData.externalVehicleDto;
import static com.tychewealth.testdata.VehicleTestData.defaultVehicle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.tychewealth.client.VehicleApiClient;
import com.tychewealth.dto.ExternalVehicleDto;
import com.tychewealth.model.Vehicle;
import com.tychewealth.scheduler.VehiclePollingScheduler;
import com.tychewealth.store.VehicleStore;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@ActiveProfiles("test")
@SpringBootTest(
    properties = {
      "mock.provider.enabled=false",
      "vehicle.polling.enabled=true",
      "vehicle.polling.initial-delay=1d",
      "vehicle.polling.fixed-delay=1d"
    })
class VehicleControllerIntegrationTest {

  @MockitoSpyBean private VehicleStore vehicleStore;

  @MockitoBean private VehicleApiClient vehicleApiClient;

  @Autowired private ApplicationContext applicationContext;

  @Autowired private VehiclePollingScheduler vehiclePollingScheduler;

  private WebTestClient webTestClient;

  @BeforeEach
  void setUp() {
    vehicleStore.replaceAll(Map.of());
    webTestClient = WebTestClient.bindToApplicationContext(applicationContext).build();
  }

  @Test
  void shouldReturnCurrentVehicles() {
    Vehicle firstVehicle = defaultVehicle(FIRST_VEHICLE_ID);
    Vehicle secondVehicle = defaultVehicle(SECOND_VEHICLE_ID);

    vehicleStore.replaceAll(
        Map.of(
            firstVehicle.id(), firstVehicle,
            secondVehicle.id(), secondVehicle));

    webTestClient
        .get()
        .uri(VEHICLES_ENDPOINT)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(Vehicle.class)
        .value(
            vehicles ->
                assertThat(vehicles)
                    .hasSize(2)
                    .extracting(Vehicle::id)
                    .containsExactlyInAnyOrder(FIRST_VEHICLE_ID, SECOND_VEHICLE_ID));
  }

  @Test
  void shouldExposeVehiclesAfterPollingFlowRuns() {
    ExternalVehicleDto externalVehicleDto = defaultExternalVehicleDto();
    ExternalVehicleDto updatedExternalVehicleDto =
        externalVehicleDto(SECOND_VEHICLE_ID, "Updated Vehicle");

    when(vehicleApiClient.fetchResources())
        .thenReturn(Mono.just(List.of(externalVehicleDto)))
        .thenReturn(Mono.just(List.of(updatedExternalVehicleDto)));

    vehiclePollingScheduler.pollVehicles();

    webTestClient
        .get()
        .uri(VEHICLES_ENDPOINT)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(Vehicle.class)
        .value(
            vehicles ->
                assertThat(vehicles)
                    .hasSize(1)
                    .extracting(Vehicle::id)
                    .containsExactly(externalVehicleDto.id()));

    vehiclePollingScheduler.pollVehicles();

    webTestClient
        .get()
        .uri(VEHICLES_ENDPOINT)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBodyList(Vehicle.class)
        .value(
            vehicles ->
                assertThat(vehicles)
                    .hasSize(1)
                    .extracting(Vehicle::id)
                    .containsExactly(updatedExternalVehicleDto.id())
                    .doesNotContain(VEHICLE_ID));
  }

  @Test
  void shouldReturnInternalServerErrorWhenFetchingVehiclesFails() {
    doThrow(new RuntimeException("Store read failed")).when(vehicleStore).getAll();

    webTestClient
        .get()
        .uri(VEHICLES_ENDPOINT)
        .exchange()
        .expectStatus()
        .is5xxServerError()
        .expectBody()
        .jsonPath("$.code")
        .isEqualTo(GENERIC_INTERNAL_ERROR.getCode())
        .jsonPath("$.type")
        .isEqualTo(GENERIC_INTERNAL_ERROR.getType())
        .jsonPath("$.description")
        .isEqualTo(GENERIC_INTERNAL_ERROR.getDescription());
  }
}

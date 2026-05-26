package com.tychewealth.service;

import static com.tychewealth.constants.TestConstants.FIRST_VEHICLE_ID;
import static com.tychewealth.constants.TestConstants.SECOND_VEHICLE_ID;
import static com.tychewealth.testdata.ClientTestData.defaultExternalVehicleDto;
import static com.tychewealth.testdata.VehicleTestData.defaultVehicle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tychewealth.client.VehicleApiClient;
import com.tychewealth.dto.ExternalVehicleDto;
import com.tychewealth.mapper.VehicleMapper;
import com.tychewealth.model.Vehicle;
import com.tychewealth.store.VehicleStore;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class VehiclePollingServiceTest {

  @Mock private VehicleApiClient vehicleApiClient;

  @Mock private VehicleMapper vehicleMapper;

  private final VehicleStore vehicleStore = new VehicleStore();
  private ExternalVehicleDto externalVehicleDto;
  private Vehicle currentVehicle;
  private VehiclePollingService vehiclePollingService;

  @BeforeEach
  void setUp() {
    externalVehicleDto = defaultExternalVehicleDto();
    currentVehicle = defaultVehicle(FIRST_VEHICLE_ID);
    vehiclePollingService =
        new VehiclePollingService(vehicleApiClient, vehicleMapper, vehicleStore);
  }

  @Test
  void shouldDetectAddedVehiclesAndUpdateStore() {
    Vehicle addedVehicle = defaultVehicle(SECOND_VEHICLE_ID);

    vehicleStore.replaceAll(Map.of(currentVehicle.id(), currentVehicle));

    mockFetchedVehicles(List.of(currentVehicle, addedVehicle));

    StepVerifier.create(vehiclePollingService.pollVehicles())
        .assertNext(
            vehicleChanges -> {
              assertThat(vehicleChanges.addedVehicles()).containsExactly(addedVehicle);
              assertThat(vehicleChanges.removedVehicles()).isEmpty();
            })
        .verifyComplete();

    assertThat(vehicleStore.getAll()).containsExactlyInAnyOrder(currentVehicle, addedVehicle);
  }

  @Test
  void shouldDetectRemovedVehiclesAndUpdateStore() {
    Vehicle removedVehicle = defaultVehicle(SECOND_VEHICLE_ID);

    vehicleStore.replaceAll(
        Map.of(
            currentVehicle.id(), currentVehicle,
            removedVehicle.id(), removedVehicle));

    mockFetchedVehicles(List.of(currentVehicle));

    StepVerifier.create(vehiclePollingService.pollVehicles())
        .assertNext(
            vehicleChanges -> {
              assertThat(vehicleChanges.addedVehicles()).isEmpty();
              assertThat(vehicleChanges.removedVehicles()).containsExactly(removedVehicle);
            })
        .verifyComplete();

    assertThat(vehicleStore.getAll()).containsExactly(currentVehicle);
  }

  @Test
  void shouldReturnNoChangesWhenVehiclesRemainTheSame() {
    vehicleStore.replaceAll(Map.of(currentVehicle.id(), currentVehicle));

    mockFetchedVehicles(List.of(currentVehicle));

    StepVerifier.create(vehiclePollingService.pollVehicles())
        .assertNext(
            vehicleChanges -> {
              assertThat(vehicleChanges.addedVehicles()).isEmpty();
              assertThat(vehicleChanges.removedVehicles()).isEmpty();
            })
        .verifyComplete();

    assertThat(vehicleStore.getAll()).containsExactly(currentVehicle);
  }

  @Test
  void shouldHandleInitialLoadAsAddedVehicles() {

    mockFetchedVehicles(List.of(currentVehicle));

    StepVerifier.create(vehiclePollingService.pollVehicles())
        .assertNext(
            vehicleChanges -> {
              assertThat(vehicleChanges.addedVehicles()).containsExactly(currentVehicle);
              assertThat(vehicleChanges.removedVehicles()).isEmpty();
            })
        .verifyComplete();

    assertThat(vehicleStore.getAll()).containsExactly(currentVehicle);
  }

  @Test
  void shouldUpdateStoreWithEmptyVehicleList() {
    Vehicle removedVehicle = defaultVehicle(SECOND_VEHICLE_ID);

    vehicleStore.replaceAll(
        Map.of(
            currentVehicle.id(), currentVehicle,
            removedVehicle.id(), removedVehicle));

    mockFetchedVehicles(List.of());

    StepVerifier.create(vehiclePollingService.pollVehicles())
        .assertNext(
            vehicleChanges -> {
              assertThat(vehicleChanges.addedVehicles()).isEmpty();
              assertThat(vehicleChanges.removedVehicles())
                  .containsExactlyInAnyOrder(currentVehicle, removedVehicle);
            })
        .verifyComplete();

    assertThat(vehicleStore.getAll()).isEmpty();
  }

  @Test
  void shouldPropagateClientErrorWithoutUpdatingStore() {
    RuntimeException error = new RuntimeException("Client error");
    Vehicle existingVehicle = currentVehicle;

    vehicleStore.replaceAll(Map.of(existingVehicle.id(), existingVehicle));

    when(vehicleApiClient.fetchResources()).thenReturn(Mono.error(error));

    StepVerifier.create(vehiclePollingService.pollVehicles())
        .expectErrorSatisfies(throwable -> assertThat(throwable).isSameAs(error))
        .verify();

    verifyNoInteractions(vehicleMapper);
    assertThat(vehicleStore.getAll()).containsExactly(existingVehicle);
  }

  private void mockFetchedVehicles(List<Vehicle> vehicles) {
    when(vehicleApiClient.fetchResources()).thenReturn(Mono.just(List.of(externalVehicleDto)));
    when(vehicleMapper.toDomainList(List.of(externalVehicleDto))).thenReturn(vehicles);
  }
}

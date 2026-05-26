package com.tychewealth.store;

import static com.tychewealth.constants.TestConstants.FIRST_VEHICLE_ID;
import static com.tychewealth.constants.TestConstants.SECOND_VEHICLE_ID;
import static com.tychewealth.testdata.VehicleTestData.defaultVehicle;
import static org.assertj.core.api.Assertions.assertThat;

import com.tychewealth.model.Vehicle;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VehicleStoreTest {

  private final VehicleStore vehicleStore = new VehicleStore();

  @Test
  void shouldStartEmpty() {
    assertThat(vehicleStore.getAll()).isEmpty();
    assertThat(vehicleStore.getCurrentVehiclesById()).isEmpty();
  }

  @Test
  void shouldReplaceAllVehicles() {
    Vehicle firstVehicle = defaultVehicle(FIRST_VEHICLE_ID);
    Vehicle secondVehicle = defaultVehicle(SECOND_VEHICLE_ID);

    vehicleStore.replaceAll(
        Map.of(
            firstVehicle.id(), firstVehicle,
            secondVehicle.id(), secondVehicle));

    assertThat(vehicleStore.getAll()).containsExactlyInAnyOrder(firstVehicle, secondVehicle);
    assertThat(vehicleStore.getCurrentVehiclesById())
        .containsEntry(firstVehicle.id(), firstVehicle)
        .containsEntry(secondVehicle.id(), secondVehicle);
  }

  @Test
  void shouldReplacePreviousStateCompletely() {
    Vehicle initialVehicle = defaultVehicle(FIRST_VEHICLE_ID);
    Vehicle replacementVehicle = defaultVehicle(SECOND_VEHICLE_ID);

    vehicleStore.replaceAll(Map.of(initialVehicle.id(), initialVehicle));
    vehicleStore.replaceAll(Map.of(replacementVehicle.id(), replacementVehicle));

    assertThat(vehicleStore.getAll()).containsExactly(replacementVehicle);
    assertThat(vehicleStore.getCurrentVehiclesById())
        .containsOnlyKeys(replacementVehicle.id())
        .containsEntry(replacementVehicle.id(), replacementVehicle);
  }
}

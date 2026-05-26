package com.tychewealth.store;

import com.tychewealth.model.Vehicle;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class VehicleStore {

  private final AtomicReference<Map<String, Vehicle>> currentVehicles =
      new AtomicReference<>(Map.of());

  public Collection<Vehicle> getAll() {
    return currentVehicles.get().values();
  }

  public Map<String, Vehicle> getCurrentVehiclesById() {
    return currentVehicles.get();
  }

  public void replaceAll(Map<String, Vehicle> vehiclesById) {
    currentVehicles.set(Map.copyOf(vehiclesById));
  }
}

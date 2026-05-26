package com.tychewealth.model;

import java.util.List;

public record VehicleChanges(List<Vehicle> addedVehicles, List<Vehicle> removedVehicles) {

  public VehicleChanges {
    addedVehicles = List.copyOf(addedVehicles);
    removedVehicles = List.copyOf(removedVehicles);
  }

  public boolean hasChanges() {
    return !addedVehicles.isEmpty() || !removedVehicles.isEmpty();
  }
}

package com.tychewealth.service;

import com.tychewealth.client.VehicleApiClient;
import com.tychewealth.mapper.VehicleMapper;
import com.tychewealth.model.Vehicle;
import com.tychewealth.model.VehicleChanges;
import com.tychewealth.model.VehiclePollingResult;
import com.tychewealth.store.VehicleStore;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class VehiclePollingService {

  private final VehicleApiClient vehicleApiClient;
  private final VehicleMapper vehicleMapper;
  private final VehicleStore vehicleStore;

  public Mono<VehicleChanges> pollVehicles() {
    return vehicleApiClient
        .fetchResources()
        .map(vehicleMapper::toDomainList)
        .map(
            polledVehicles -> {
              Map<String, Vehicle> currentVehicles = vehicleStore.getCurrentVehiclesById();
              Map<String, Vehicle> newVehicles = mapVehiclesById(polledVehicles);

              List<Vehicle> addedVehicles = findVehicles(newVehicles, currentVehicles);
              List<Vehicle> removedVehicles = findVehicles(currentVehicles, newVehicles);

              return new VehiclePollingResult(
                  new VehicleChanges(addedVehicles, removedVehicles), newVehicles);
            })
        .doOnNext(vehiclePollingResult -> vehicleStore.replaceAll(vehiclePollingResult.newState()))
        .map(VehiclePollingResult::changes);
  }

  private Map<String, Vehicle> mapVehiclesById(List<Vehicle> vehicles) {
    Map<String, Vehicle> vehiclesById = new LinkedHashMap<>();
    vehicles.forEach(vehicle -> vehiclesById.put(vehicle.id(), vehicle));
    return vehiclesById;
  }

  private List<Vehicle> findVehicles(
      Map<String, Vehicle> sourceVehiclesById, Map<String, Vehicle> referenceVehiclesById) {
    return sourceVehiclesById.entrySet().stream()
        .filter(entry -> !referenceVehiclesById.containsKey(entry.getKey()))
        .map(Map.Entry::getValue)
        .toList();
  }
}

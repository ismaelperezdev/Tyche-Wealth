package com.tychewealth.controller;

import com.tychewealth.model.Vehicle;
import com.tychewealth.store.VehicleStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicles", description = "Get which vehicles are available in the selected area")
public class VehicleController {

  private final VehicleStore vehicleStore;

  @GetMapping
  @Operation(
      summary = "Get available vehicles",
      description =
          "Returns the latest list of vehicles available according to the most recent polling cycle")
  @ApiResponse(responseCode = "200", description = "Vehicles retrieved successfully")
  @ApiResponse(responseCode = "500", description = "Unexpected server error")
  public Collection<Vehicle> getVehicles() {
    return vehicleStore.getAll();
  }
}

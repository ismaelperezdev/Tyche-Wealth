package com.tychewealth.testdata;

import com.tychewealth.dto.ExternalVehicleDto;
import java.util.List;

public final class EntityBuilder {

  private EntityBuilder() {}

  public static ExternalVehicleDto buildExternalVehicleDto(
      String id,
      String name,
      double x,
      double y,
      String licencePlate,
      int range,
      int helmets,
      String resourceImageId,
      String resourceUrl,
      List<String> resourcesImagesUrls,
      boolean realTimeData,
      String resourceType,
      int companyZoneId) {
    return new ExternalVehicleDto(
        id,
        name,
        x,
        y,
        licencePlate,
        range,
        helmets,
        resourceImageId,
        resourceUrl,
        resourcesImagesUrls,
        realTimeData,
        resourceType,
        companyZoneId);
  }
}

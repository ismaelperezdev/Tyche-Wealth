package com.tychewealth.testdata;

import com.tychewealth.config.properties.ResourcesClientProperties;
import com.tychewealth.dto.ExternalVehicleDto;
import java.util.List;
import java.util.stream.Collectors;

public final class ClientTestData {

  public static final String VEHICLE_ID = "demo-vehicle-001";
  public static final String VEHICLE_NAME = "DEMO-001";
  public static final double VEHICLE_X = -3.70256;
  public static final double VEHICLE_Y = 40.41831;
  public static final String LICENCE_PLATE = "DEMO-001";
  public static final int RANGE = 23;
  public static final int HELMETS = 2;
  public static final String RESOURCE_IMAGE_ID = "vehicle_demo_moped";
  public static final String RESOURCE_URL = "https://example.test/vehicles/demo-vehicle-001";
  public static final List<String> RESOURCE_IMAGES_URLS = List.of("vehicle_demo_moped");
  public static final boolean REAL_TIME_DATA = true;
  public static final String RESOURCE_TYPE = "MOPED";
  public static final int COMPANY_ZONE_ID = 102;

  public static final String RESOURCES_RESPONSE =
      """
            [
              {
                "id": "%s",
                "name": "%s",
                "x": %s,
                "y": %s,
                "licencePlate": "%s",
                "range": %s,
                "helmets": %s,
                "resourceImageId": "%s",
                "resourceUrl": "%s",
                "resourcesImagesUrls": ["vehicle_demo_moped"],
                "realTimeData": %s,
                "resourceType": "%s",
                "companyZoneId": %s
              }
            ]
            """
          .formatted(
              VEHICLE_ID,
              VEHICLE_NAME,
              VEHICLE_X,
              VEHICLE_Y,
              LICENCE_PLATE,
              RANGE,
              HELMETS,
              RESOURCE_IMAGE_ID,
              RESOURCE_URL,
              REAL_TIME_DATA,
              RESOURCE_TYPE,
              COMPANY_ZONE_ID);

  private ClientTestData() {}

  public static ExternalVehicleDto defaultExternalVehicleDto() {
    return externalVehicleDto(VEHICLE_ID, VEHICLE_NAME);
  }

  public static ExternalVehicleDto externalVehicleDto(String vehicleId, String vehicleName) {
    return EntityBuilder.buildExternalVehicleDto(
        vehicleId,
        vehicleName,
        VEHICLE_X,
        VEHICLE_Y,
        LICENCE_PLATE,
        RANGE,
        HELMETS,
        RESOURCE_IMAGE_ID,
        RESOURCE_URL,
        RESOURCE_IMAGES_URLS,
        REAL_TIME_DATA,
        RESOURCE_TYPE,
        COMPANY_ZONE_ID);
  }

  public static String expectedUri(ResourcesClientProperties properties) {
    return properties.baseUrl()
        + properties.resourcesPath()
        + "?lowerLeftLatLon="
        + properties.lowerLeftLatLon()
        + "&upperRightLatLon="
        + properties.upperRightLatLon()
        + "&companyZoneIds="
        + properties.companyZoneIds().stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));
  }
}

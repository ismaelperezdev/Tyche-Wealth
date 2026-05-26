package com.tychewealth.mock.enums;

public enum ResourceImageId {
  VEHICLE_DEMO_MOPED("vehicle_demo_moped"),
  VEHICLE_DEMO_BIKE("vehicle_demo_bike"),
  VEHICLE_DEMO_SCOOTER("vehicle_demo_scooter");

  private final String value;

  ResourceImageId(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }
}

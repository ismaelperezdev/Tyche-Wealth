package com.tychewealth.testdata;

import com.tychewealth.model.Vehicle;
import com.tychewealth.model.VehicleLocation;

public final class VehicleTestData {

  public static final String DEFAULT_VEHICLE_NAME = "Vehicle";
  public static final String DEFAULT_VEHICLE_TYPE = "MOPED";
  public static final double DEFAULT_LATITUDE = 40.41831;
  public static final double DEFAULT_LONGITUDE = -3.70256;
  public static final String DEFAULT_LICENCE_PLATE = "DEMO-001";
  public static final int DEFAULT_RANGE = 23;
  public static final int DEFAULT_HELMETS = 2;
  public static final boolean DEFAULT_REAL_TIME_DATA = true;
  public static final int DEFAULT_COMPANY_ZONE_ID = 102;

  public static Vehicle defaultVehicle(String id) {
    return new Vehicle(
        id,
        DEFAULT_VEHICLE_NAME + " " + id,
        DEFAULT_VEHICLE_TYPE,
        new VehicleLocation(DEFAULT_LATITUDE, DEFAULT_LONGITUDE),
        DEFAULT_LICENCE_PLATE,
        DEFAULT_RANGE,
        DEFAULT_HELMETS,
        DEFAULT_REAL_TIME_DATA,
        DEFAULT_COMPANY_ZONE_ID);
  }
}

package com.tychewealth.mock.enums;

public enum CompanyZoneId {
  ZONE_100(100),
  ZONE_101(101),
  ZONE_102(102);

  private final int value;

  CompanyZoneId(int value) {
    this.value = value;
  }

  public int value() {
    return value;
  }
}

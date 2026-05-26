package com.tychewealth.model;

public record Vehicle(
    String id,
    String name,
    String type,
    VehicleLocation location,
    String licencePlate,
    int range,
    int helmets,
    boolean realTimeData,
    int companyZoneId) {}

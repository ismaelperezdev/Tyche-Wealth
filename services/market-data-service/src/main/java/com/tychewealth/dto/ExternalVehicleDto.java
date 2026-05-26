package com.tychewealth.dto;

import java.util.List;

public record ExternalVehicleDto(
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
    int companyZoneId) {}

package com.tychewealth.model;

import java.util.Map;

public record VehiclePollingResult(VehicleChanges changes, Map<String, Vehicle> newState) {}

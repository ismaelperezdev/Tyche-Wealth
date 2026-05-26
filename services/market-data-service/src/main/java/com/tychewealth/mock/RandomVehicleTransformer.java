package com.tychewealth.mock;

import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.tychewealth.config.properties.MockProviderProperties;
import com.tychewealth.dto.ExternalVehicleDto;
import com.tychewealth.mock.helper.RandomVehicleHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

public class RandomVehicleTransformer implements ResponseDefinitionTransformerV2 {

  private static final String NAME = "random-vehicle-resources-transformer";

  private final MockProviderProperties properties;
  private final MockVehicleGenerator mockVehicleGenerator = new MockVehicleGenerator();
  private final RandomVehicleHelper helper;
  private final ReentrantLock stateLock = new ReentrantLock();
  private final List<ExternalVehicleDto> currentVehicles = new ArrayList<>();
  private volatile String currentResponseBody;
  private volatile long nextRefreshAtEpochMillis;

  public RandomVehicleTransformer(MockProviderProperties properties) {
    this.properties = properties;
    this.helper = new RandomVehicleHelper(properties);
    initializeCurrentVehicles();
    this.currentResponseBody = mockVehicleGenerator.serializeVehicles(currentVehicles);
    this.nextRefreshAtEpochMillis = helper.nextRefreshAtEpochMillis();
  }

  @Override
  public ResponseDefinition transform(ServeEvent serveEvent) {

    ThreadLocalRandom random = ThreadLocalRandom.current();
    int removable = currentVehicles.size() - properties.minVehiclesPerResponse();
    int addable = properties.maxVehiclesPerResponse() - currentVehicles.size();
    long refreshedAt = System.currentTimeMillis();

    if (System.currentTimeMillis() < nextRefreshAtEpochMillis) {
      return helper.jsonResponse(currentResponseBody);
    }

    stateLock.lock();
    try {

      if (refreshedAt < nextRefreshAtEpochMillis) {
        return helper.jsonResponse(currentResponseBody);
      }

      if (removable > 0) {
        helper.repeatBoundedVehicleChanges(
            random,
            removable,
            index -> currentVehicles.remove(random.nextInt(currentVehicles.size())));
      }

      if (addable > 0) {
        helper.repeatBoundedVehicleChanges(
            random, addable, index -> currentVehicles.add(mockVehicleGenerator.newVehicle()));
      }

      currentResponseBody = mockVehicleGenerator.serializeVehicles(currentVehicles);
      nextRefreshAtEpochMillis = helper.nextRefreshAtEpochMillis();
    } finally {
      stateLock.unlock();
    }

    return helper.jsonResponse(currentResponseBody);
  }

  @Override
  public String getName() {
    return NAME;
  }

  private void initializeCurrentVehicles() {
    int totalVehicles =
        ThreadLocalRandom.current()
            .nextInt(properties.minVehiclesPerResponse(), properties.maxVehiclesPerResponse() + 1);
    helper.repeat(totalVehicles, index -> currentVehicles.add(mockVehicleGenerator.newVehicle()));
  }
}

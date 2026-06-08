package com.tychewealth.mock.helper;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.tychewealth.config.properties.MockProviderProperties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public class RandomVehicleHelper {

  private final MockProviderProperties properties;

  public RandomVehicleHelper(MockProviderProperties properties) {
    this.properties = properties;
  }

  public long nextRefreshAtEpochMillis() {
    int durationSeconds =
        ThreadLocalRandom.current()
            .nextInt(
                properties.minResponseDurationSeconds(),
                properties.maxResponseDurationSeconds() + 1);
    return System.currentTimeMillis() + durationSeconds * 1000L;
  }

  public void repeat(int totalTimes, IntConsumer action) {
    IntStream.range(0, totalTimes).forEach(action);
  }

  public void repeatBoundedVehicleChanges(
      ThreadLocalRandom random, int maxAllowedChanges, IntConsumer action) {
    repeat(
        Math.min(
            random.nextInt(
                properties.minVehiclesChangedPerRefresh(),
                properties.maxVehiclesChangedPerRefresh() + 1),
            maxAllowedChanges),
        action);
  }

  public ResponseDefinition jsonResponse(String responseBody) {
    return ResponseDefinitionBuilder.responseDefinition()
        .withStatus(HttpStatus.OK.value())
        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .withBody(responseBody)
        .build();
  }
}

package com.tychewealth.mock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.dto.ExternalVehicleDto;
import com.tychewealth.mock.enums.CompanyZoneId;
import com.tychewealth.mock.enums.ResourceImageId;
import com.tychewealth.mock.enums.ResourceType;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class MockVehicleGenerator {

  private static final char DIGIT_START = '0';
  private static final char UPPERCASE_LETTER_START = 'A';
  private static final double COORDINATE_PRECISION_FACTOR = 100000d;
  private static final String VEHICLE_NAME_PREFIX = "VEHICLE-";

  private final ObjectMapper objectMapper;

  public MockVehicleGenerator() {
    this.objectMapper = new ObjectMapper();
  }

  public ExternalVehicleDto newVehicle() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    String id = UUID.randomUUID().toString();
    String imageId = randomEnum(random, ResourceImageId.values()).value();
    String resourceType = randomEnum(random, ResourceType.values()).name();
    int companyZoneId = randomEnum(random, CompanyZoneId.values()).value();
    String licencePrefix = randomString(random, 4, UPPERCASE_LETTER_START, 26);

    return new ExternalVehicleDto(
        id,
        VEHICLE_NAME_PREFIX + randomString(random, 4, DIGIT_START, 10),
        randomCoordinate(random, -3.7100, -3.6900),
        randomCoordinate(random, 40.4100, 40.4300),
        licencePrefix + randomString(random, 3, DIGIT_START, 10),
        random.nextInt(10, 121),
        random.nextInt(1, 3),
        imageId,
        "https://mocked.vehicles.local/resources/" + id,
        List.of(imageId),
        random.nextBoolean(),
        resourceType,
        companyZoneId);
  }

  public String serializeVehicles(List<ExternalVehicleDto> vehicles) {
    try {
      return objectMapper.writeValueAsString(vehicles);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("Unable to serialize random vehicle response", error);
    }
  }

  private String randomString(
      ThreadLocalRandom random, int length, char startChar, int alphabetSize) {
    return IntStream.range(0, length)
        .map(index -> startChar + random.nextInt(alphabetSize))
        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
        .toString();
  }

  private <T> T randomEnum(ThreadLocalRandom random, T[] values) {
    return values[random.nextInt(values.length)];
  }

  private double randomCoordinate(ThreadLocalRandom random, double min, double max) {
    return Math.round(random.nextDouble(min, max) * COORDINATE_PRECISION_FACTOR)
        / COORDINATE_PRECISION_FACTOR;
  }
}

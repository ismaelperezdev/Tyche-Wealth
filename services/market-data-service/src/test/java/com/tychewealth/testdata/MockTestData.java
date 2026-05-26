package com.tychewealth.testdata;

import com.tychewealth.config.properties.MockProviderProperties;
import com.tychewealth.config.properties.ResourcesClientProperties;
import com.tychewealth.mock.enums.CompanyZoneId;
import com.tychewealth.mock.enums.ResourceImageId;
import com.tychewealth.mock.enums.ResourceType;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

public final class MockTestData {

  public static final String MOCK_RESOURCE_URL_PREFIX = "https://mocked.vehicles.local/resources/";
  public static final String VEHICLE_NAME_PATTERN = "VEHICLE-\\d{4}";
  public static final String LICENCE_PLATE_PATTERN = "[A-Z]{4}\\d{3}";
  public static final double MIN_X_COORDINATE = -3.7100;
  public static final double MAX_X_COORDINATE = -3.6900;
  public static final double MIN_Y_COORDINATE = 40.4100;
  public static final double MAX_Y_COORDINATE = 40.4300;
  public static final int MIN_RANGE = 10;
  public static final int MAX_RANGE = 120;
  public static final int MIN_HELMETS = 1;
  public static final int MAX_HELMETS = 2;
  public static final int MOCK_PROVIDER_PORT = 18089;
  public static final int MIN_VEHICLES_PER_RESPONSE = 1;
  public static final int MAX_VEHICLES_PER_RESPONSE = 2;
  public static final int MIN_VEHICLES_CHANGED_PER_REFRESH = 0;
  public static final int MAX_VEHICLES_CHANGED_PER_REFRESH = 1;
  public static final int MIN_RESPONSE_DURATION_SECONDS = 1;
  public static final int MAX_RESPONSE_DURATION_SECONDS = 1;
  public static final String MOCK_BASE_URL = "http://localhost:" + MOCK_PROVIDER_PORT;
  public static final String MOCK_RESOURCES_PATH = "/external-api/resources";
  public static final String LOWER_LEFT_LAT_LON = "40.4168,-3.7038";
  public static final String UPPER_RIGHT_LAT_LON = "40.4268,-3.6938";
  public static final String MOCK_USER_AGENT = "vehicle-availability-service-test/1.0";
  public static final int MAX_RETRIES = 2;
  public static final Duration REQUEST_TIMEOUT = Duration.ofMillis(100);
  public static final Duration RETRY_BACKOFF = Duration.ofMillis(10);

  public static final Set<String> RESOURCE_IMAGE_IDS =
      Arrays.stream(ResourceImageId.values())
          .map(ResourceImageId::value)
          .collect(Collectors.toSet());
  public static final Set<String> RESOURCE_TYPES =
      Arrays.stream(ResourceType.values()).map(Enum::name).collect(Collectors.toSet());
  public static final Set<Integer> COMPANY_ZONE_IDS =
      Arrays.stream(CompanyZoneId.values()).map(CompanyZoneId::value).collect(Collectors.toSet());

  public static MockProviderProperties defaultMockProviderProperties() {
    return new MockProviderProperties(
        true,
        MOCK_PROVIDER_PORT,
        MIN_VEHICLES_PER_RESPONSE,
        MAX_VEHICLES_PER_RESPONSE,
        MIN_VEHICLES_CHANGED_PER_REFRESH,
        MAX_VEHICLES_CHANGED_PER_REFRESH,
        MIN_RESPONSE_DURATION_SECONDS,
        MAX_RESPONSE_DURATION_SECONDS);
  }

  public static ResourcesClientProperties defaultResourcesClientProperties() {
    return new ResourcesClientProperties(
        MOCK_BASE_URL,
        MOCK_RESOURCES_PATH,
        LOWER_LEFT_LAT_LON,
        UPPER_RIGHT_LAT_LON,
        List.of(100, 101, 102),
        MOCK_USER_AGENT,
        REQUEST_TIMEOUT,
        MAX_RETRIES,
        RETRY_BACKOFF);
  }

  public static Stream<Arguments> invalidMockProviderProperties() {
    return Stream.of(
        Arguments.of(
            invalidMinVehiclesPerResponse(),
            "mock.provider.min-vehicles-per-response must be greater than zero"),
        Arguments.of(
            invalidMaxVehiclesPerResponseLessThanMin(),
            "mock.provider.max-vehicles-per-response must be greater than or equal to min-vehicles-per-response"),
        Arguments.of(
            invalidMaxVehiclesChangedLessThanMin(),
            "mock.provider.max-vehicles-changed-per-refresh must be greater than or equal to min-vehicles-changed-per-refresh"),
        Arguments.of(
            invalidMinResponseDuration(),
            "mock.provider.min-response-duration-seconds must be greater than zero"),
        Arguments.of(
            invalidMaxResponseDurationLessThanMin(),
            "mock.provider.max-response-duration-seconds must be greater than or equal to min-response-duration-seconds"));
  }

  public static MockProviderProperties invalidMinVehiclesPerResponse() {
    return new MockProviderProperties(
        true,
        MOCK_PROVIDER_PORT,
        0,
        MAX_VEHICLES_PER_RESPONSE,
        MIN_VEHICLES_CHANGED_PER_REFRESH,
        MAX_VEHICLES_CHANGED_PER_REFRESH,
        MIN_RESPONSE_DURATION_SECONDS,
        MAX_RESPONSE_DURATION_SECONDS);
  }

  public static MockProviderProperties invalidMaxVehiclesPerResponseLessThanMin() {
    return new MockProviderProperties(
        true,
        MOCK_PROVIDER_PORT,
        MAX_VEHICLES_PER_RESPONSE,
        MIN_VEHICLES_PER_RESPONSE,
        MIN_VEHICLES_CHANGED_PER_REFRESH,
        MAX_VEHICLES_CHANGED_PER_REFRESH,
        MIN_RESPONSE_DURATION_SECONDS,
        MAX_RESPONSE_DURATION_SECONDS);
  }

  public static MockProviderProperties invalidMaxVehiclesChangedLessThanMin() {
    return new MockProviderProperties(
        true,
        MOCK_PROVIDER_PORT,
        MIN_VEHICLES_PER_RESPONSE,
        MAX_VEHICLES_PER_RESPONSE,
        MAX_VEHICLES_CHANGED_PER_REFRESH,
        MIN_VEHICLES_CHANGED_PER_REFRESH,
        MIN_RESPONSE_DURATION_SECONDS,
        MAX_RESPONSE_DURATION_SECONDS);
  }

  public static MockProviderProperties invalidMinResponseDuration() {
    return new MockProviderProperties(
        true,
        MOCK_PROVIDER_PORT,
        MIN_VEHICLES_PER_RESPONSE,
        MAX_VEHICLES_PER_RESPONSE,
        MIN_VEHICLES_CHANGED_PER_REFRESH,
        MAX_VEHICLES_CHANGED_PER_REFRESH,
        0,
        MAX_RESPONSE_DURATION_SECONDS);
  }

  public static MockProviderProperties invalidMaxResponseDurationLessThanMin() {
    return new MockProviderProperties(
        true,
        MOCK_PROVIDER_PORT,
        MIN_VEHICLES_PER_RESPONSE,
        MAX_VEHICLES_PER_RESPONSE,
        MIN_VEHICLES_CHANGED_PER_REFRESH,
        MAX_VEHICLES_CHANGED_PER_REFRESH,
        MAX_RESPONSE_DURATION_SECONDS + 1,
        MAX_RESPONSE_DURATION_SECONDS);
  }

  private MockTestData() {}
}

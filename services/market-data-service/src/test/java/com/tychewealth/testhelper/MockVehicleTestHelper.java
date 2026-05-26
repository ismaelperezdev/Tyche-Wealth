package com.tychewealth.testhelper;

import static com.tychewealth.testdata.MockTestData.COMPANY_ZONE_IDS;
import static com.tychewealth.testdata.MockTestData.LICENCE_PLATE_PATTERN;
import static com.tychewealth.testdata.MockTestData.MAX_HELMETS;
import static com.tychewealth.testdata.MockTestData.MAX_RANGE;
import static com.tychewealth.testdata.MockTestData.MAX_VEHICLES_PER_RESPONSE;
import static com.tychewealth.testdata.MockTestData.MAX_X_COORDINATE;
import static com.tychewealth.testdata.MockTestData.MAX_Y_COORDINATE;
import static com.tychewealth.testdata.MockTestData.MIN_HELMETS;
import static com.tychewealth.testdata.MockTestData.MIN_RANGE;
import static com.tychewealth.testdata.MockTestData.MIN_VEHICLES_PER_RESPONSE;
import static com.tychewealth.testdata.MockTestData.MIN_X_COORDINATE;
import static com.tychewealth.testdata.MockTestData.MIN_Y_COORDINATE;
import static com.tychewealth.testdata.MockTestData.MOCK_RESOURCE_URL_PREFIX;
import static com.tychewealth.testdata.MockTestData.RESOURCE_IMAGE_IDS;
import static com.tychewealth.testdata.MockTestData.RESOURCE_TYPES;
import static com.tychewealth.testdata.MockTestData.VEHICLE_NAME_PATTERN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.ImmutableRequest;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.tychewealth.dto.ExternalVehicleDto;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public final class MockVehicleTestHelper {

  public static ServeEvent mockServeEvent(String absoluteUrl) {
    return ServeEvent.of(
        ImmutableRequest.create()
            .withMethod(RequestMethod.GET)
            .withAbsoluteUrl(absoluteUrl)
            .build());
  }

  public static void assertValidVehicleResponse(
      ResponseDefinition response, ObjectMapper objectMapper) throws Exception {
    List<ExternalVehicleDto> vehicles =
        objectMapper.readValue(response.getBody(), new TypeReference<>() {});

    assertSoftly(
        softly -> {
          softly.assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
          softly
              .assertThat(response.getHeaders().getHeader(HttpHeaders.CONTENT_TYPE).firstValue())
              .isEqualTo(MediaType.APPLICATION_JSON_VALUE);
          softly
              .assertThat(vehicles)
              .hasSizeBetween(MIN_VEHICLES_PER_RESPONSE, MAX_VEHICLES_PER_RESPONSE);
          softly.assertThat(vehicles).allSatisfy(MockVehicleTestHelper::assertVehicleShape);
        });
  }

  public static void assertVehicleShape(ExternalVehicleDto vehicle) {
    assertThat(vehicle.id()).isNotBlank();
    assertThat(vehicle.name()).matches(VEHICLE_NAME_PATTERN);
    assertThat(vehicle.x()).isBetween(MIN_X_COORDINATE, MAX_X_COORDINATE);
    assertThat(vehicle.y()).isBetween(MIN_Y_COORDINATE, MAX_Y_COORDINATE);
    assertThat(vehicle.licencePlate()).matches(LICENCE_PLATE_PATTERN);
    assertThat(vehicle.range()).isBetween(MIN_RANGE, MAX_RANGE);
    assertThat(vehicle.helmets()).isBetween(MIN_HELMETS, MAX_HELMETS);
    assertThat(vehicle.resourceImageId()).isIn(RESOURCE_IMAGE_IDS);
    assertThat(vehicle.resourceUrl()).isEqualTo(MOCK_RESOURCE_URL_PREFIX + vehicle.id());
    assertThat(vehicle.resourcesImagesUrls()).containsExactly(vehicle.resourceImageId());
    assertThat(vehicle.resourceType()).isIn(RESOURCE_TYPES);
    assertThat(vehicle.companyZoneId()).isIn(COMPANY_ZONE_IDS);
  }

  private MockVehicleTestHelper() {}
}

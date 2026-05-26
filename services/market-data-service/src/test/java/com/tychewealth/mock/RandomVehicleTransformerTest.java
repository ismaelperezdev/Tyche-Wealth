package com.tychewealth.mock;

import static com.tychewealth.testdata.MockTestData.defaultMockProviderProperties;
import static com.tychewealth.testhelper.MockVehicleTestHelper.assertValidVehicleResponse;
import static com.tychewealth.testhelper.MockVehicleTestHelper.mockServeEvent;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RandomVehicleTransformerTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String MOCK_ABSOLUTE_URL = "http://localhost/external-api/resources";

  private final RandomVehicleTransformer randomVehicleTransformer =
      new RandomVehicleTransformer(defaultMockProviderProperties());

  @Test
  void shouldExposeExpectedTransformerName() {
    assertThat(randomVehicleTransformer.getName())
        .isEqualTo("random-vehicle-resources-transformer");
  }

  @Test
  void shouldReturnJsonResponseWithVehiclesWithinConfiguredRange() throws Exception {
    ResponseDefinition response =
        randomVehicleTransformer.transform(mockServeEvent(MOCK_ABSOLUTE_URL));

    assertValidVehicleResponse(response, OBJECT_MAPPER);
  }

  @Test
  void shouldReturnSameResponseBeforeRefreshWindowExpires() {
    ResponseDefinition firstResponse =
        randomVehicleTransformer.transform(mockServeEvent(MOCK_ABSOLUTE_URL));
    ResponseDefinition secondResponse =
        randomVehicleTransformer.transform(mockServeEvent(MOCK_ABSOLUTE_URL));

    assertThat(secondResponse.getBody()).isEqualTo(firstResponse.getBody());
  }

  @Test
  void shouldReturnValidResponseAfterRefreshWindowExpires() throws Exception {
    randomVehicleTransformer.transform(mockServeEvent(MOCK_ABSOLUTE_URL));

    ReflectionTestUtils.setField(randomVehicleTransformer, "nextRefreshAtEpochMillis", 0L);

    ResponseDefinition refreshedResponse =
        randomVehicleTransformer.transform(mockServeEvent(MOCK_ABSOLUTE_URL));

    assertValidVehicleResponse(refreshedResponse, OBJECT_MAPPER);
  }
}

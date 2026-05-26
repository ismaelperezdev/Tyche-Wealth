package com.tychewealth.mock.helper;

import static com.tychewealth.testdata.MockTestData.MAX_RESPONSE_DURATION_SECONDS;
import static com.tychewealth.testdata.MockTestData.MIN_RESPONSE_DURATION_SECONDS;
import static com.tychewealth.testdata.MockTestData.defaultMockProviderProperties;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class RandomVehicleHelperTest {

  private static final String RESPONSE_BODY = "[{\"id\":\"vehicle-1\"}]";

  private final RandomVehicleHelper randomVehicleHelper =
      new RandomVehicleHelper(defaultMockProviderProperties());

  @Test
  void shouldReturnNextRefreshTimeWithinConfiguredRange() {
    long beforeCall = System.currentTimeMillis();
    long nextRefreshAtEpochMillis = randomVehicleHelper.nextRefreshAtEpochMillis();
    long afterCall = System.currentTimeMillis();

    assertThat(nextRefreshAtEpochMillis)
        .isBetween(
            beforeCall + MIN_RESPONSE_DURATION_SECONDS * 1000L,
            afterCall + MAX_RESPONSE_DURATION_SECONDS * 1000L);
  }

  @Test
  void shouldRepeatActionGivenNumberOfTimes() {
    AtomicInteger executions = new AtomicInteger();

    randomVehicleHelper.repeat(3, index -> executions.incrementAndGet());

    assertThat(executions).hasValue(3);
  }

  @Test
  void shouldNotRepeatActionWhenTotalTimesIsZero() {
    AtomicInteger executions = new AtomicInteger();

    randomVehicleHelper.repeat(0, index -> executions.incrementAndGet());

    assertThat(executions).hasValue(0);
  }

  @Test
  void shouldNotRepeatVehicleChangesMoreThanAllowedMaximum() {
    AtomicInteger executions = new AtomicInteger();

    randomVehicleHelper.repeatBoundedVehicleChanges(
        ThreadLocalRandom.current(), 0, index -> executions.incrementAndGet());

    assertThat(executions).hasValue(0);
  }

  @Test
  void shouldBuildJsonResponseWithExpectedMetadata() {
    var response = randomVehicleHelper.jsonResponse(RESPONSE_BODY);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeaders().getHeader(HttpHeaders.CONTENT_TYPE).firstValue())
        .isEqualTo(MediaType.APPLICATION_JSON_VALUE);
    assertThat(response.getBody()).isEqualTo(RESPONSE_BODY);
  }
}

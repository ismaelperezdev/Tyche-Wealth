package com.tychewealth.marketdataclient.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class MarketDataClientPropertiesTest {

  @Test
  void appliesDefaultTimeoutsWhenTheyAreNotConfigured() {
    MarketDataClientProperties properties =
        new MarketDataClientProperties("http://localhost:8081", null, null);

    assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
    assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(5));
  }

  @Test
  void rejectsBlankBaseUrl() {
    assertThatThrownBy(() -> new MarketDataClientProperties(" ", null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("clients.market-data.base-url must not be blank");
  }

  @Test
  void rejectsNonPositiveTimeouts() {
    assertThatThrownBy(
            () ->
                new MarketDataClientProperties(
                    "http://localhost:8081", Duration.ZERO, Duration.ofSeconds(1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("clients.market-data.connect-timeout must be positive");

    assertThatThrownBy(
            () ->
                new MarketDataClientProperties(
                    "http://localhost:8081", Duration.ofSeconds(1), Duration.ofSeconds(-1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("clients.market-data.read-timeout must be positive");
  }
}

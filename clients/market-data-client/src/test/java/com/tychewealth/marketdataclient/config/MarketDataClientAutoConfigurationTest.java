package com.tychewealth.marketdataclient.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.tychewealth.marketdataclient.MarketDataClient;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

class MarketDataClientAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(MarketDataClientAutoConfiguration.class))
          .withBean(RestClient.Builder.class, RestClient::builder);

  @Test
  void doesNotCreateClientWhenBaseUrlIsNotConfigured() {
    contextRunner.run(context -> assertThat(context).doesNotHaveBean(MarketDataClient.class));
  }

  @Test
  void createsClientWithDefaultTimeoutsWhenBaseUrlIsConfigured() {
    contextRunner
        .withPropertyValues("clients.market-data.base-url=http://localhost:8081")
        .run(
            context -> {
              assertThat(context).hasSingleBean(MarketDataClient.class);
              MarketDataClientProperties properties =
                  context.getBean(MarketDataClientProperties.class);
              assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(2));
              assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(5));
            });
  }

  @Test
  void bindsConfiguredTimeouts() {
    contextRunner
        .withPropertyValues(
            "clients.market-data.base-url=http://localhost:8081",
            "clients.market-data.connect-timeout=3s",
            "clients.market-data.read-timeout=7s")
        .run(
            context -> {
              MarketDataClientProperties properties =
                  context.getBean(MarketDataClientProperties.class);
              assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
              assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(7));
            });
  }

  @Test
  void usesApplicationProvidedClientWhenPresent() {
    MarketDataClient customClient = new MarketDataClient(RestClient.builder().build());

    contextRunner
        .withPropertyValues("clients.market-data.base-url=http://localhost:8081")
        .withBean(MarketDataClient.class, () -> customClient)
        .run(context -> assertThat(context.getBean(MarketDataClient.class)).isSameAs(customClient));
  }
}

package com.tychewealth.marketdataclient.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/** Connection settings for the internal market-data service. */
@ConfigurationProperties("clients.market-data")
public record MarketDataClientProperties(
    String baseUrl, Duration connectTimeout, Duration readTimeout) {

  private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(2);
  private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(5);

  public MarketDataClientProperties {
    if (!StringUtils.hasText(baseUrl)) {
      throw new IllegalArgumentException("clients.market-data.base-url must not be blank");
    }
    connectTimeout = connectTimeout == null ? DEFAULT_CONNECT_TIMEOUT : connectTimeout;
    readTimeout = readTimeout == null ? DEFAULT_READ_TIMEOUT : readTimeout;
    if (connectTimeout.isNegative() || connectTimeout.isZero()) {
      throw new IllegalArgumentException("clients.market-data.connect-timeout must be positive");
    }
    if (readTimeout.isNegative() || readTimeout.isZero()) {
      throw new IllegalArgumentException("clients.market-data.read-timeout must be positive");
    }
  }
}

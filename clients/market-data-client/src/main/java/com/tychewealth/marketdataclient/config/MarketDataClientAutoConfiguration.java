package com.tychewealth.marketdataclient.config;

import com.tychewealth.marketdataclient.MarketDataClient;
import java.net.http.HttpClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Creates the market-data HTTP client when its base URL is configured. */
@AutoConfiguration(after = RestClientAutoConfiguration.class)
@ConditionalOnClass(RestClient.class)
@ConditionalOnProperty(prefix = "clients.market-data", name = "base-url")
@EnableConfigurationProperties(MarketDataClientProperties.class)
public class MarketDataClientAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  MarketDataClient marketDataClient(
      RestClient.Builder restClientBuilder, MarketDataClientProperties properties) {
    HttpClient httpClient =
        HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(properties.readTimeout());

    RestClient restClient =
        restClientBuilder.baseUrl(properties.baseUrl()).requestFactory(requestFactory).build();
    return new MarketDataClient(restClient);
  }
}

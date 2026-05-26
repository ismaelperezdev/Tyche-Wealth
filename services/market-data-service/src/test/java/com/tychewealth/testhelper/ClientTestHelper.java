package com.tychewealth.testhelper;

import com.tychewealth.client.VehicleApiClient;
import com.tychewealth.config.properties.ResourcesClientProperties;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

public final class ClientTestHelper {

  public static VehicleApiClient createClient(
      ExchangeFunction exchangeFunction, ResourcesClientProperties resourcesClientProperties) {
    return new VehicleApiClient(
        WebClient.builder().exchangeFunction(exchangeFunction), resourcesClientProperties);
  }
}

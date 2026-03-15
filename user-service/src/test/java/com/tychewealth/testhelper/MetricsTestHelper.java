package com.tychewealth.testhelper;

import io.micrometer.core.instrument.MeterRegistry;

public final class MetricsTestHelper {

  private MetricsTestHelper() {}

  public static double counterValue(MeterRegistry meterRegistry, String counterName) {
    var counter = meterRegistry.find(counterName).counter();
    return counter == null ? 0 : counter.count();
  }
}

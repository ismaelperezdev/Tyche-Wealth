package com.tychewealth.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.EnumMap;
import java.util.function.Function;

abstract class BaseMetrics<E extends Enum<E>> {

  private final EnumMap<E, Counter> counters;

  protected BaseMetrics(
      Class<E> metricType,
      MeterRegistry meterRegistry,
      Function<E, String> metricName,
      Function<E, String> description) {
    this.counters = new EnumMap<>(metricType);
    for (E metric : metricType.getEnumConstants()) {
      counters.put(
          metric,
          Counter.builder(metricName.apply(metric))
              .description(description.apply(metric))
              .register(meterRegistry));
    }
  }

  public void incrementMetric(E metric) {
    counters.get(metric).increment();
  }

  public void incrementMetricBy(E metric, double count) {
    if (count > 0) {
      counters.get(metric).increment(count);
    }
  }
}

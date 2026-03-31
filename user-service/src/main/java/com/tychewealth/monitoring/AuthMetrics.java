package com.tychewealth.monitoring;

import com.tychewealth.enums.AuthMetricEnum;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AuthMetrics extends BaseMetrics<AuthMetricEnum> {

  public AuthMetrics(MeterRegistry meterRegistry) {
    super(
        AuthMetricEnum.class,
        meterRegistry,
        AuthMetricEnum::metricName,
        AuthMetricEnum::description);
  }
}

package com.tychewealth.monitoring;

import com.tychewealth.enums.UserMetricEnum;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class UserMetrics extends BaseMetrics<UserMetricEnum> {

  public UserMetrics(MeterRegistry meterRegistry) {
    super(
        UserMetricEnum.class,
        meterRegistry,
        UserMetricEnum::metricName,
        UserMetricEnum::description);
  }
}

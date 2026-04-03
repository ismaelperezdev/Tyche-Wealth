package com.tychewealth.controller;

import com.tychewealth.ratelimit.RateLimitKey;
import com.tychewealth.testhelper.InMemoryRateLimitStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

class PortfolioRateLimitIntegrationTest {

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @ParameterizedTest
  @MethodSource("com.tychewealth.testdata.EndpointTestData#rateLimitedEndpoints")
  void endpointReturnsTooManyRequestsWhenRateLimitIsExceeded(
      RateLimitKey rateLimitKey, MockHttpServletRequest request, HandlerMethod handlerMethod) {
    InMemoryRateLimitStore.assertRateLimited(rateLimitKey, request, handlerMethod);
  }
}

package com.tychewealth.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tychewealth.dto.ratelimit.PortfolioRateLimitPropertiesDto;
import com.tychewealth.ratelimit.RateLimitInterceptor;
import com.tychewealth.ratelimit.support.PortfolioRateLimitSupport;
import com.tychewealth.testhelper.InMemoryRateLimitStore;
import java.time.Clock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

class PortfolioRateLimitIntegrationTest {

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createReturnsTooManyRequestsWhenRateLimitIsExceeded() {
    InMemoryRateLimitStore rateLimitStore = new InMemoryRateLimitStore(Clock.systemUTC());
    PortfolioRateLimitSupport portfolioRateLimitSupport =
        new PortfolioRateLimitSupport(rateLimitStore);
    RateLimitInterceptor interceptor =
        portfolioRateLimitSupport.buildCreatePortfolioInterceptor(
            new PortfolioRateLimitPropertiesDto.RateLimitDto(1, 60));

    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(42L, null));

    MockHttpServletRequest firstRequest =
        new MockHttpServletRequest("POST", "/tyche-wealth/portfolio-service/v1/portfolio");
    MockHttpServletRequest secondRequest =
        new MockHttpServletRequest("POST", "/tyche-wealth/portfolio-service/v1/portfolio");
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertDoesNotThrow(() -> interceptor.preHandle(firstRequest, response, new Object()));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> interceptor.preHandle(secondRequest, response, new Object()));

    assertEquals(429, exception.getStatusCode().value());
  }
}

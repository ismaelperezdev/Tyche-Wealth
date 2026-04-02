package com.tychewealth;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TycheWealthPortfolioServiceApplicationTest {

  @Test
  void contextLoads() {
    assertNotNull(TycheWealthPortfolioServiceApplication.class);
  }
}

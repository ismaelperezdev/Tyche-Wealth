package com.tychewealth;

import static com.tychewealth.constants.TestConstants.TEST_JWT_SECRET;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    properties = {
      "app.auth.jwt.secret=" + TEST_JWT_SECRET,
      "spring.main.allow-bean-definition-overriding=true",
      "spring.data.redis.repositories.enabled=false"
    })
@ActiveProfiles("test")
class TycheWealthPortfolioServiceApplicationTest {}

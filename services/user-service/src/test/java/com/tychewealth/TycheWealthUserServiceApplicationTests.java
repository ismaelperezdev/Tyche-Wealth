package com.tychewealth;

import com.tychewealth.config.IntegrationTestConfig;
import com.tychewealth.config.TestDatabaseConfig;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@EnableAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
@Import({IntegrationTestConfig.class, TestDatabaseConfig.class})
public class TycheWealthUserServiceApplicationTests {}

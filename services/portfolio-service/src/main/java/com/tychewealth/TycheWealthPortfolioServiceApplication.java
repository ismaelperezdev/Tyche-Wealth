package com.tychewealth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class TycheWealthPortfolioServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(TycheWealthPortfolioServiceApplication.class, args);
  }
}

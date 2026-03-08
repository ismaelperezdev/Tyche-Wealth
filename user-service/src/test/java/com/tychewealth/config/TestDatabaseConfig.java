package com.tychewealth.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestDatabaseConfig {

  @Bean
  @Primary
  public DataSource dataSource() {
    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setDriverClassName("org.h2.Driver");
    dataSource.setJdbcUrl(
        "jdbc:h2:mem:tyche_it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
    dataSource.setUsername("sa");
    dataSource.setPassword("");
    dataSource.setMaximumPoolSize(5);
    dataSource.setMinimumIdle(1);
    dataSource.setPoolName("tyche-test-hikari");
    return dataSource;
  }

  @Bean
  public HibernatePropertiesCustomizer hibernateCreateDropCustomizer() {
    return properties -> properties.put("hibernate.hbm2ddl.auto", "create-drop");
  }
}

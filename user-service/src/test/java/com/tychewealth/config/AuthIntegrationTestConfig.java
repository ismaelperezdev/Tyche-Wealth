package com.tychewealth.config;

import com.tychewealth.controller.impl.AuthApiController;
import com.tychewealth.entity.RefreshTokenEntity;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.handler.ErrorHandler;
import com.tychewealth.mapper.user.UserMapper;
import com.tychewealth.repository.RefreshTokenRepository;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.helper.*;
import com.tychewealth.service.impl.AuthServiceImpl;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@Import(IntegrationTestConfig.class)
@ComponentScan(
    basePackageClasses = {
      AuthApiController.class,
      AuthServiceImpl.class,
      AuthValidationHelper.class,
      AuthRegisterHelper.class,
      AuthLoginHelper.class,
      AuthTokenHelper.class,
      AuthRefreshTokenHelper.class,
      ErrorHandler.class,
      UserMapper.class
    })
@EnableJpaRepositories(basePackageClasses = {UserRepository.class, RefreshTokenRepository.class})
@EntityScan(basePackageClasses = {UserEntity.class, RefreshTokenEntity.class})
public class AuthIntegrationTestConfig {}

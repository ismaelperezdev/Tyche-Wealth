package com.tychewealth.config;

import com.tychewealth.controller.impl.AuthApiController;
import com.tychewealth.error.handler.ErrorHandler;
import com.tychewealth.service.helper.auth.AuthRegisterHelper;
import com.tychewealth.service.helper.auth.AuthValidationHelper;
import com.tychewealth.service.helper.token.TokenValidationHelper;
import com.tychewealth.service.impl.AuthServiceImpl;
import com.tychewealth.service.monitoring.UserMetrics;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import({
  IntegrationTestConfig.class,
  AuthApiController.class,
  AuthServiceImpl.class,
  AuthValidationHelper.class,
  TokenValidationHelper.class,
  AuthRegisterHelper.class,
  UserMetrics.class,
  ErrorHandler.class
})
public class AuthTestConfig {}

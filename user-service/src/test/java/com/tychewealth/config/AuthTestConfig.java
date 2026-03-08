package com.tychewealth.config;

import com.tychewealth.controller.impl.AuthApiController;
import com.tychewealth.error.handler.ErrorHandler;
import com.tychewealth.service.helper.AuthRegisterHelper;
import com.tychewealth.service.helper.AuthValidationHelper;
import com.tychewealth.service.impl.AuthServiceImpl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import({
  IntegrationTestConfig.class,
  AuthApiController.class,
  AuthServiceImpl.class,
  AuthValidationHelper.class,
  AuthRegisterHelper.class,
  ErrorHandler.class
})
public class AuthTestConfig {}

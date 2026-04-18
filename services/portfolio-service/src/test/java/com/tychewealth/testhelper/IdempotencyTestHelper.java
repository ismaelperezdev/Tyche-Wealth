package com.tychewealth.testhelper;

import com.tychewealth.controller.impl.AssetApiController;
import com.tychewealth.controller.impl.PortfolioApiController;
import com.tychewealth.service.AssetService;
import com.tychewealth.service.PortfolioService;
import org.mockito.Mockito;
import org.springframework.web.method.HandlerMethod;

public final class IdempotencyTestHelper {

  private IdempotencyTestHelper() {}

  public static HandlerMethod portfolio(String methodName, Class<?>... parameterTypes)
      throws NoSuchMethodException {
    return new HandlerMethod(
        new PortfolioApiController(Mockito.mock(PortfolioService.class)),
        PortfolioApiController.class.getMethod(methodName, parameterTypes));
  }

  public static HandlerMethod asset(String methodName, Class<?>... parameterTypes)
      throws NoSuchMethodException {
    return new HandlerMethod(
        new AssetApiController(Mockito.mock(AssetService.class)),
        AssetApiController.class.getMethod(methodName, parameterTypes));
  }
}

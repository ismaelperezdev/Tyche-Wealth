package com.tychewealth.constants;

public final class RedisConstants {

  public static final String EMAIL_DAILY_LIMIT_NAMESPACE = "rate-limit:email:daily";
  public static final String EMAIL_DAILY_LIMIT_CLIENT_KEY = "global";
  public static final String AUTH_LOGIN_DEVICE_EMAIL_COOLDOWN_NAMESPACE =
      "cooldown:auth:login-device-email";

  private RedisConstants() {}
}

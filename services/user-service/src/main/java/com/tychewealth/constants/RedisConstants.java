package com.tychewealth.constants;

public final class RedisConstants {

  public static final String EMAIL_DAILY_LIMIT_NAMESPACE = "rate-limit:email:daily";
  public static final String EMAIL_DAILY_LIMIT_CLIENT_KEY = "global";
  public static final String AUTH_LOGIN_DEVICE_EMAIL_COOLDOWN_NAMESPACE =
      "cooldown:auth:login-device-email";
  public static final String ACTIVE_USERS_KEY = "users:active";
  public static final String ACTIVE_USERS_LAST_REFRESH_KEY = "users:active:last-refresh";
  public static final String ACTIVE_USERS_TEMP_KEY_PREFIX = "users:active:tmp:";

  private RedisConstants() {}
}

package com.tychewealth.constants;

import org.springframework.data.redis.core.script.DefaultRedisScript;

public final class RedisConstants {

  public static final String EMAIL_DAILY_LIMIT_NAMESPACE = "rate-limit:email:daily";
  public static final String EMAIL_DAILY_LIMIT_CLIENT_KEY = "global";
  public static final String AUTH_LOGIN_DEVICE_EMAIL_COOLDOWN_NAMESPACE =
      "cooldown:auth:login-device-email";

  public static final DefaultRedisScript<Long> INCREMENT_WITH_TTL_SCRIPT =
      new DefaultRedisScript<>(
          """
          local current = redis.call('INCR', KEYS[1])
          if current == 1 then
            redis.call('PEXPIRE', KEYS[1], ARGV[1])
          end
          return current
          """,
          Long.class);

  private RedisConstants() {}
}

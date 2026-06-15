package com.tychewealth.redis;

import org.springframework.data.redis.core.script.DefaultRedisScript;

public final class RedisScripts {

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

  private RedisScripts() {}
}

package com.tychewealth.constants;

import org.springframework.data.redis.core.script.DefaultRedisScript;

public final class RedisConstants {

  public static final String ASSET_IMPORT_AI_CACHE_KEY_PREFIX = "asset-import:ai:";
  public static final String ASSET_IMPORT_PAYLOAD_CACHE_KEY_PREFIX = "asset-import:payload:";
  public static final String ASSET_IMPORT_INFLIGHT_KEY_PREFIX =
      "asset-import:payload:inflight:";
  public static final String ASSET_IMPORT_RESULT_KEY_PREFIX = "asset-import:result:";

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

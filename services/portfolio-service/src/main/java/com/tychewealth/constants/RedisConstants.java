package com.tychewealth.constants;

public final class RedisConstants {

  public static final String ACTIVE_SYMBOLS_KEY = "active-symbols";
  public static final String ACTIVE_SYMBOLS_TEMP_KEY_PREFIX = "active-symbols:temp:";
  public static final String ASSET_IMPORT_PAYLOAD_CACHE_KEY_PREFIX = "asset-import:payload:";
  public static final String ASSET_IMPORT_INFLIGHT_KEY_PREFIX = "asset-import:payload:inflight:";
  public static final String ASSET_IMPORT_RESULT_KEY_PREFIX = "asset-import:result:";

  private RedisConstants() {}
}

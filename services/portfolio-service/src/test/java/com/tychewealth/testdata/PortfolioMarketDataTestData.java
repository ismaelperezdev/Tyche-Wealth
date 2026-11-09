package com.tychewealth.testdata;

import java.time.Instant;

public final class PortfolioMarketDataTestData {

  public static final Instant CHECKPOINT_BEFORE_LATER_ASSET = Instant.parse("2026-10-10T00:00:00Z");
  public static final Instant CHECKPOINT_AT_LATER_ASSET_CREATION =
      Instant.parse("2026-10-15T00:00:00Z");
  public static final Instant EARLY_ASSET_CREATED_AT = Instant.parse("2026-10-01T00:00:00Z");
  public static final Instant HISTORY_FROM = Instant.parse("2026-09-01T00:00:00Z");
  public static final Instant HISTORY_TO = Instant.parse("2026-11-01T00:00:00Z");

  private PortfolioMarketDataTestData() {}
}

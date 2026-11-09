package com.tychewealth.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AssetEntityTest {

  @Test
  void lifecycleTimestampsAreGeneratedInUtc() {
    AssetEntity asset = new AssetEntity();
    LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC);

    asset.onCreate();

    LocalDateTime after = LocalDateTime.now(ZoneOffset.UTC);
    assertTrue(!asset.getCreatedAt().isBefore(before) && !asset.getCreatedAt().isAfter(after));
    assertEquals(asset.getCreatedAt(), asset.getUpdatedAt());

    before = LocalDateTime.now(ZoneOffset.UTC);
    asset.onUpdate();
    after = LocalDateTime.now(ZoneOffset.UTC);
    assertTrue(!asset.getUpdatedAt().isBefore(before) && !asset.getUpdatedAt().isAfter(after));
  }
}

package com.tychewealth.model;

import java.time.Instant;
import java.util.List;

/** Reconstructed portfolio positions at one timestamp. */
public record PortfolioPositionSnapshot(
    Instant timestamp, List<PortfolioValuationPosition> positions) {

  public PortfolioPositionSnapshot {
    positions = List.copyOf(positions);
  }
}

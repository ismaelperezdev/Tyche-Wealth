package com.tychewealth.kafka.events;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ActiveSymbolChanges(
    UUID eventId, Instant occurredAt, Set<String> addedSymbols, Set<String> removedSymbols) {}

package com.tychewealth.kafka.events;

import java.util.Set;

public record ActiveSymbolChanges(Set<String> addedSymbols, Set<String> removedSymbols) {}

package com.tychewealth.kafka.events;

import java.time.Instant;
import java.util.Set;

public record ActiveUsersEvent(Instant occurredAt, Set<Long> userIds) {}

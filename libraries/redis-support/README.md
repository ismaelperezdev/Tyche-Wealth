# Redis Support

Shared Redis integration for Tyche Wealth services. It provides a string-serialized
`RedisTemplate<String, String>` and a Redis-backed counter store through `RateLimitStore`.

## Features

`RedisRateLimitStore` implements `increment`, `clear`, and `resetNamespace`. Increment uses an
atomic Lua script that sets the TTL when the counter is created, avoiding lost updates between
concurrent requests.

The library provides storage primitives; each service remains responsible for its own quotas,
client keys, and rejection policy.

## Usage

Add `tyche-wealth-redis-support` as a Maven dependency and configure Spring Data Redis to provide a
`RedisConnectionFactory`. Ensure component scanning includes `com.tychewealth` (or import
`RedisConfig` and register `RedisRateLimitStore`), then inject the contract where needed:

```java
long requestCount = rateLimitStore.increment("email", clientKey, Duration.ofDays(1));
```

The consuming service must include and configure its Spring Data Redis dependencies. This module is
shared runtime support, not a standalone application.

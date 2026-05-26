# Market Data Service

## Overview

`market-data-service` is the Tyche Wealth microservice responsible for retrieving, refreshing and exposing market data used by portfolio analytics and charting features.

The service is being integrated incrementally from a previous polling architecture proof of concept and is now aligned with the Tyche Wealth monorepo structure.

## Current Integration Status

- Integrated as a Maven module in the monorepo parent build.
- Aligned to the shared parent dependency and plugin management.
- Package structure aligned to `com.tychewealth.*`.
- Swagger/OpenAPI metadata adapted to market data domain.
- Legacy polling logic is still present and will be replaced progressively.

## Target Responsibilities

- Retrieve unique symbols from portfolio holdings.
- Poll external market data providers on a fixed schedule.
- Cache the latest market snapshots to reduce provider cost and rate-limit pressure.
- Expose read-optimized endpoints for consumers (portfolio/chart clients).

## Planned Architecture

1. Symbol source layer (`portfolio-service` integration or dedicated symbol source).
2. Provider client layer (resilient HTTP client with timeout/retry/backoff).
3. Polling scheduler (periodic refresh, no per-user provider calls).
4. Market data store (Redis-backed cache and optional historical persistence).
5. API layer for read access by internal consumers.

## Run Locally

From this directory:

```powershell
.\mvnw spring-boot:run
```

Or from the repository root:

```powershell
.\mvnw -pl services/market-data-service spring-boot:run
```

## Build

```powershell
.\mvnw -pl services/market-data-service -DskipTests compile
```

## Notes

- This module is in migration phase from a previous demo domain to Tyche Wealth market data domain.
- During migration, endpoint names and internal classes may change before final stabilization.

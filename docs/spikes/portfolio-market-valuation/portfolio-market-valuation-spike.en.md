# Spike: Portfolio Market Valuation

Spanish version: [portfolio-market-valuation-spike.es.md](portfolio-market-valuation-spike.es.md).

## Investigation

This spike records the agreed direction for calculating a portfolio's historical unrealized profit or loss. It is not the gross market value of its holdings. Each point uses the quantity and `averagePrice` in effect at that time together with the market price; amounts are reported in the portfolio's base currency. `AssetEntity.averagePrice` remains the average acquisition cost denominated in `AssetEntity.currency` and is not modified. Scope, calculation, currency, missing-data, and route decisions are closed in this spike; remaining details are implementation concerns.

The existing public market-data API is `PortfolioMarketDataApi` at `services/portfolio-service/src/main/java/com/tychewealth/controller/PortfolioMarketDataApi.java`. It exposes latest-price and history reads. `PortfolioMarketDataServiceImpl` validates the selection, resolves active assets owned by the authenticated user through `PortfolioRepository` and `AssetRepository`, deduplicates symbols, and calls MDS through `MarketDataClient`.

Portfolio is the appropriate place to compose a valuation: it owns the portfolio, asset quantities, ownership checks, and `PortfolioEntity.baseCurrency`. MDS owns market observations. The existing synchronous Portfolio → MDS flow and service-to-service authentication provide a natural path for retrieving them. MDS should not be exposed directly to the frontend.

MDS currently stores recent quotes in Redis through `MarketQuoteStore`, with a ten-minute TTL, and persists periodic six-hour checkpoints. `MarketQuoteService` captures and stores quotes; `MarketPriceReadServiceImpl` reads recent quotes from Redis and historical prices from persisted checkpoints. By agreement, MDS market prices and checkpoints are always interpreted as EUR. This unit is part of the semantic contract and does not require a currency field in their DTOs.

When a recent quote is unavailable, valuation should use the applicable persisted checkpoint, retaining its actual capture time. If neither a quote nor a checkpoint exists for a point where the portfolio held a position, the entire request fails. `AssetEntity.averagePrice` is not used to replace a missing market price.

A portfolio may contain assets in different currencies. Convert market prices from EUR to `PortfolioEntity.baseCurrency`. Frankfurter is the FX source and covers all currencies available in the product. MDS retrieves daily rates in batches, persists them, and exposes an internal batch read; Portfolio does not maintain a second FX cache. Use the latest FX rate available when the response is calculated for the entire historical series, applying the same rate to every point rather than a historical rate for each date. If no rate is available for a required pair, fail the entire request; do not invent a 1:1 rate or return a partial chart. Retain the effective date of the applied rate.

The scope is a historical series of unrealized profit or loss from a selected start date through the present. Portfolio reconstructs each date's quantity and cost basis from `AssetVariationEntity` events and combines them with prices from MDS `historic` and `latest` endpoints: history for past points and latest for the current point. For each point, per-asset unrealized P/L is `quantity × (market price converted to base currency − averagePrice converted to base currency)`. Portfolio returns the total unrealized P/L amount and total acquisition cost (`sum of quantity × averagePrice`, converted to base currency) per point, both in base currency; the frontend derives the percentage as P/L divided by acquisition cost. If total acquisition cost is zero, the percentage cannot be calculated. The percentage is neither persisted nor calculated by Portfolio. Gross market value (`quantity × price`) is not this chart's metric. For dates and sampling, follow the existing Portfolio `/history` endpoint style: optional `from` and `to` `Instant` parameters, an effective start no earlier than the asset's creation, and MDS's existing history resolution/sampling. All points use the latest FX rates available when the series is generated, for both market price and cost basis. Apply variations according to `occurredAt`: each point uses the latest state at or before its timestamp. Twelve Data has not been selected as the price provider.

To reconstruct the requested range, Portfolio needs each asset's latest state at or before `from` as the initial state, plus subsequent variations through `to`. Include only assets that had a position during the range, including assets deleted after it; exclude assets created after `to` or deleted before `from`. The variation query must not filter on `AssetEntity.deletedAt`, but it remains scoped to the `portfolioId` whose ownership Portfolio has already validated.

## Problems and Constraints

- **Authorization:** Valuation must use the authenticated Portfolio boundary and verify that `{portfolioId}` belongs to the user. Historical lookup includes currently deleted assets that held a position during the range; it must not be restricted to the active-asset selector used by existing market-data reads. MDS calls retain existing service-to-service authentication.
- **Different currencies:** MDS market prices are denominated in EUR and are converted to the containing portfolio's `baseCurrency`.
- **Cost basis is not market price:** `averagePrice` is the cost basis for unrealized P/L; it must not be represented as a market quote or used to replace a missing market price.
- **Redis expiry:** A missing Redis quote does not establish that a symbol has never had market data. MDS must check persisted checkpoints; if no price exists for a position at that point, the entire request fails.
- **Stale observations:** A checkpoint fallback is a real but potentially stale observation. Preserve its capture time and distinguish it from a recent quote.
- **Missing required data:** If a required price or FX rate is missing after applying the allowed fallbacks, fail the entire valuation request. Do not substitute zero or a fabricated rate, and do not return a partial chart.
- **FX dates:** Frankfurter rates are daily. Use the latest rate available when generating the response for the entire series, applying it to every point, and retain its effective date. Do not reconstruct FX by historical point date or imply intraday accuracy.
- **MDS price unit:** MDS quotes and checkpoints are considered EUR by contract; these DTOs do not need a currency field. FX data requires persistence and a Liquibase migration in MDS.
- **Batching:** Request distinct symbols and currency pairs in batches. Avoid making an MDS or FX call per asset.
- **No write side effects:** Valuation must not mutate assets, `averagePrice`, or portfolio state.
- **No public MDS exposure:** The frontend continues to call Portfolio, not MDS or the Frankfurter API.

## Possible Options

### Option: Use only recent quotes

#### Approach

Calculate an asset's value only when a recent MDS quote is available. Convert it to the portfolio currency and mark the valuation incomplete if a quote or required conversion is missing.

#### Expected Changes

Extend Portfolio's valuation operation and response DTOs, and add the required batch reads to `MarketDataClient`. MDS market prices are interpreted as EUR by contract; no currency field or asset persistence change is needed.

#### Advantages

- Values are based only on market observations.
- Keeps acquisition cost separate from market price.

#### Disadvantages

- If neither a quote nor a checkpoint exists yet for a position, valuation fails until MDS has an observation.
- Does not use a persisted checkpoint when the recent Redis quote is unavailable.

### Option: Use acquisition cost whenever market data is missing

#### Approach

Use `quantity × averagePrice` as a general fallback whenever a quote is absent.

#### Expected Changes

Portfolio could calculate a fallback without consulting MDS checkpoints, but the response would need to identify the value's source.

#### Advantages

- Provides a value for more assets when market data is unavailable.

#### Disadvantages

- Treats acquisition cost as a substitute for current market data, potentially hiding stale data or provider failures.
- Conflates acquisition cost with market value and can produce a misleading chart.

### Option: Prefer recent quote, then checkpoint; fail when price is missing — selected

#### Approach

For each point, reconstruct quantity and `averagePrice` by applying variations up to that timestamp. Use MDS `historic` prices for past points and `latest` for the current point; retain actual timestamps and use checkpoints as applicable. Calculate unrealized P/L as `quantity × (market price − averagePrice)`, converting both prices to the portfolio's base currency with current FX rates. If a market price or required FX rate is missing, fail the entire request; `averagePrice` never replaces the market price.

#### Expected Changes

Extend MDS reads to support checkpoint fallback. Market prices and checkpoints are interpreted as EUR by contract, with no currency field added. Add MDS FX rate ingestion, persistence, and internal batch reads. Extend `MarketDataClient` and Portfolio's valuation orchestration and response DTOs. Do not change `averagePrice` semantics or update behavior.

#### Advantages

- Distinguishes recent quotes from stale observations without mixing acquisition cost with market price.
- Keeps MDS responsible for market observations and rates, while Portfolio remains responsible for authorization and valuation composition.

#### Disadvantages

- If MDS has no observation for a historical position, the chart cannot be generated until one is available.
- Requires persisted FX rates and coordinated internal contract changes.
- The request fails when required market or FX data is unavailable after applying the agreed fallbacks; no partial chart is returned.

### Option: Have Portfolio call Frankfurter directly or maintain a second FX cache

#### Approach

Portfolio could call Frankfurter during valuation, or maintain a local FX cache alongside MDS's persisted rates.

#### Expected Changes

Direct calls would add provider configuration, network handling, and availability concerns to Portfolio. A second cache would duplicate FX storage and lifecycle responsibilities.

#### Advantages

- Direct calls could remove the MDS dependency for FX reads.
- A local cache could make Portfolio less dependent on MDS rate-read availability.

#### Disadvantages

- Direct calls couple Portfolio to an external provider and its response behavior.
- Duplicate caches create competing freshness and effective-date behavior and duplicate ingestion responsibilities.

## Chosen Solution

### Decision

Use the quote → checkpoint flow, with no cost-based estimate, while MDS owns market observations and FX reference rates. Portfolio remains the authenticated boundary that composes the valuation and requests needed observations and rates in batches.

Reconstruct the quantity and `averagePrice` in effect at each point from `AssetVariationEntity`, applying variations whose `occurredAt` is less than or equal to the point timestamp. Retrieve past prices from `historic` and the current price from `latest`, preserving timestamps and using the applicable checkpoint according to MDS behavior. Calculate per-asset P/L as `quantity × (market price − averagePrice)`, converting both values to `PortfolioEntity.baseCurrency` using the latest FX rate available when generating the series. If a market price or required FX rate is missing, fail the entire request; do not return a partial series or substitute `averagePrice` for a price.

If required market or FX data is missing after applying the agreed fallbacks, the request fails and returns no partial valuation. Do not fabricate rates. `AssetEntity.averagePrice` is not modified.

This fits the existing Portfolio → MDS client and service-authentication flow, keeps data ownership aligned with the existing service boundaries, and avoids exposing provider details or maintaining a second FX cache. When required data is missing, an explicit error is preferred over a partial chart that could be mistaken for a valid one.

### Detailed Design

#### Existing Classes to Modify

**`PortfolioApi`**  
Path: `services/portfolio-service/src/main/java/com/tychewealth/controller/PortfolioApi.java`

Add the authenticated operation `GET /api/v1/portfolio/me/{portfolioId}/valuation` here, with optional `from` and `to` `Instant` query parameters as its only query inputs. Portfolio resolves the portfolio's assets and variations automatically and verifies ownership by the authenticated user; the client does not send asset IDs. Do not expose an MDS endpoint directly.

**`PortfolioApiController`**  
Path: `services/portfolio-service/src/main/java/com/tychewealth/controller/impl/PortfolioApiController.java`

Implement the valuation HTTP entry point here and delegate execution to the appropriate Portfolio service, preserving the project's thin-controller convention. Existing `PortfolioMarketDataApi` routes are unchanged.

**`PortfolioMarketDataService` and `PortfolioMarketDataServiceImpl`**  
Paths: `services/portfolio-service/src/main/java/com/tychewealth/service/PortfolioMarketDataService.java`, `services/portfolio-service/src/main/java/com/tychewealth/service/impl/PortfolioMarketDataServiceImpl.java`

Valuation logic receives the portfolio validated by `PortfolioApi`, loads its historical variations—including variations for deleted assets—and queries MDS for `historic`, `latest`, and required FX pairs in batches. It calculates unrealized P/L and acquisition cost per point, and fails with a typed error if required data is missing. Preserve service-token flow and do not write asset data.

**`PortfolioMarketDataHelper` and valuation response DTOs**  
Paths: `services/portfolio-service/src/main/java/com/tychewealth/service/helper/marketdata/PortfolioMarketDataHelper.java`, `services/portfolio-service/src/main/java/com/tychewealth/dto/marketdata/`

The helper currently adapts MDS price results and expands them to assets. Extend it, or add a narrowly scoped helper if needed, for valuation mapping and calculation. The response should distinguish quotes from checkpoints and retain observation and FX effective dates. If a required value remains missing after applying the fallbacks, the service throws a typed error; that error's contract remains to be settled.

**`MarketDataClient`**  
Path: `clients/market-data-client/src/main/java/com/tychewealth/marketdataclient/MarketDataClient.java`

This concrete `RestClient` wrapper currently reads latest prices and history. Add authenticated internal batch reads for market observations and FX rates, using the existing service bearer-token pattern. Keep the public Portfolio contract independent of MDS routes and provider formats.

**MDS quote model and read path**  
Relevant paths include:

- `services/market-data-service/src/main/java/com/tychewealth/model/MarketQuote.java`
- `services/market-data-service/src/main/java/com/tychewealth/dto/marketprice/LatestPriceResponseDto.java`
- `services/market-data-service/src/main/java/com/tychewealth/service/MarketQuoteService.java`
- `services/market-data-service/src/main/java/com/tychewealth/store/MarketQuoteStore.java`
- `services/market-data-service/src/main/java/com/tychewealth/service/impl/MarketPriceReadServiceImpl.java`

Quotes and checkpoints contain a price and capture time; their unit is EUR by contract, not by a DTO field. Recent quotes are read from Redis; checkpoints are persisted in PostgreSQL. Provide the latest persisted observation when no recent Redis quote is available. Preserve the observation's real capture time and distinguish it from a recent quote. Do not alter the established six-hour history/checkpoint behavior.

**MDS FX ingestion, persistence, and internal read**  
Relevant paths include `services/market-data-service/src/main/java/com/tychewealth/` and `services/market-data-service/src/main/resources/db.changelog/changelog-master.xml`.

Add daily batch retrieval from Frankfurter, persistence of rates with their currencies and effective date, and an authenticated internal batch read. Add the required model, repository, provider/service components, configuration, Liquibase changeset, and tests following MDS conventions. MDS owns retrieval and stored-rate reads; Portfolio does not persist or cache a second copy.

**`AssetEntity` and `PortfolioEntity`**  
Paths: `services/portfolio-service/src/main/java/com/tychewealth/entity/AssetEntity.java`, `services/portfolio-service/src/main/java/com/tychewealth/entity/PortfolioEntity.java`

No valuation write responsibility is added. `AssetEntity.currency` remains the source currency for `averagePrice`; `PortfolioEntity.baseCurrency` remains the target currency for reporting. Existing active-asset and ownership queries remain authoritative.

#### New Classes to Create

No generic strategy, factory, or new valuation module is required by this decision. Add only the concrete MDS components needed for FX retrieval, persistence, and internal reading, following existing service, repository, and Liquibase patterns.

A separate Portfolio valuation component is optional if implementation shows that adding the calculation to the existing orchestration/helper boundary materially harms clarity. It is not required up front.

#### Component Interaction and Flow

1. The authenticated user calls Portfolio's unrealized P/L series operation using the existing portfolio/asset selection model.
2. Portfolio validates the request and resolves only active assets belonging to that user, including each containing portfolio's `baseCurrency`.
3. Portfolio reconstructs each asset's quantity and `averagePrice` at each date by applying `AssetVariationEntity` chronologically. It requests past prices from MDS `historic` and uses `latest` for the current point, retaining actual timestamps.
4. For each point and asset with a holding, calculate `quantity × (market price − averagePrice)`. If the market price is missing, fail the entire request; cost basis is not a fallback.
5. Portfolio batches FX pairs needed to convert EUR market prices and `AssetEntity.currency` cost basis to the portfolio base currency. Use the latest available FX rate when generating the response across all points; same-currency conversion needs no external rate.
6. Portfolio sums per-asset unrealized P/L in base currency. If a required FX rate is missing, fail the entire request. Return no partial series and update no assets.
7. Portfolio returns the series through its authenticated API with point dates, price source/timestamps, and FX effective date. Missing required data is reported through a typed error.

#### Implementation Outline

1. Implement `GET /api/v1/portfolio/me/{portfolioId}/valuation?from=...&to=...`, the response schema, and a typed error for missing required data, following existing API and error-handling conventions.
2. Define how MDS reports a recent quote versus the applicable checkpoint for each historical point.
3. Apply the convention that MDS prices and checkpoints are denominated in EUR; add MDS's Frankfurter daily batch ingestion, rate persistence, internal batch read, Liquibase migration, and tests.
4. Extend `MarketDataClient` with authenticated batch reads for observations and rates.
5. Add Portfolio valuation orchestration and batched currency conversion, with explicit errors for missing required data and no mutation of `averagePrice`.
6. Test ownership and active-asset filtering; multiple currencies; quote, checkpoint, and missing-data paths; FX effective dates and missing rates; same-currency conversion; and errors without partial responses. Update generated API documentation if the public contract changes.

## Spike Status

The design decisions are closed. Exact JSON field names and HTTP mapping for the typed error will be settled during implementation by reusing the existing error contract and `ErrorHandler`; they do not block closing this spike.

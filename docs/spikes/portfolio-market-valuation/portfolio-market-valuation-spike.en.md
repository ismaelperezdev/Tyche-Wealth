# Spike: Portfolio Market Valuation

Spanish version: [portfolio-market-valuation-spike.es.md](portfolio-market-valuation-spike.es.md).

## Investigation

This spike records the agreed direction for calculating the current market value of a user's portfolio. The calculation uses active assets and their quantities, reports amounts in the owning portfolio's base currency, and does not modify `AssetEntity.averagePrice`. That field remains the asset's average acquisition cost, denominated in `AssetEntity.currency`.

The existing public market-data API is `PortfolioMarketDataApi` at `services/portfolio-service/src/main/java/com/tychewealth/controller/PortfolioMarketDataApi.java`. It exposes latest-price and history reads. `PortfolioMarketDataServiceImpl` validates the selection, resolves active assets owned by the authenticated user through `PortfolioRepository` and `AssetRepository`, deduplicates symbols, and calls MDS through `MarketDataClient`.

Portfolio is the appropriate place to compose a valuation: it owns the portfolio, asset quantities, ownership checks, and `PortfolioEntity.baseCurrency`. MDS owns market observations. The existing synchronous Portfolio → MDS flow and service-to-service authentication provide a natural path for retrieving them. MDS should not be exposed directly to the frontend.

MDS currently stores recent quotes in Redis through `MarketQuoteStore`, with a ten-minute TTL, and persists periodic six-hour checkpoints. `MarketQuoteService` captures and stores quotes; `MarketPriceReadServiceImpl` reads recent quotes from Redis and historical prices from persisted checkpoints. The current `MarketQuote` and latest-price DTO contain a symbol, price, and capture time, but no currency. For the valuation flow, MDS is initially assumed to report prices in EUR; the quote currency must be explicit in the internal contract.

When a recent quote is unavailable, valuation should use the latest persisted checkpoint, retaining its actual capture time. During initial market-data bootstrap only, if neither a quote nor a checkpoint exists, `quantity × averagePrice` may be used as an explicitly identified estimate—not as a market quote. In other cases without a price, the asset's value remains absent.

A portfolio may contain assets in different currencies. Convert both market prices and acquisition-cost estimates from their source currencies to `PortfolioEntity.baseCurrency`. Frankfurter is the proposed FX source: MDS retrieves daily rates in batches, persists them, and exposes an internal batch read. Portfolio requests the rates it needs from MDS when calculating valuation and does not maintain a second FX cache. Use the effective date returned with each rate. Do not invent a 1:1 rate or return an apparently complete total when a required price or conversion is missing.

This spike addresses current valuation, not historical portfolio reconstruction. Historical price behavior and checkpoint downsampling remain governed by the existing history spikes. Twelve Data has not been selected as the price provider, and the Frankfurter proposal is independent of that choice.

## Problems and Constraints

- **Authorization:** Valuation must use the existing authenticated Portfolio boundary and resolve only active assets owned by the user. It must not bypass the ownership checks in `PortfolioMarketDataServiceImpl` or the existing service-to-service authentication used for MDS calls.
- **Different currencies:** Each asset's market price is denominated in the quote currency; an acquisition-cost estimate is denominated in `AssetEntity.currency`. Convert either to the containing portfolio's `baseCurrency`.
- **Cost basis is not market price:** `averagePrice` must not be overwritten or represented as a market quote. Its use as an estimate is restricted to initial market-data bootstrap.
- **Redis expiry:** A missing Redis quote does not establish that a symbol has never had market data. MDS must check persisted checkpoints before allowing the bootstrap estimate.
- **Stale observations:** A checkpoint fallback is a real but potentially stale observation. Preserve its capture time and distinguish it from a recent quote.
- **Incomplete results:** Missing prices or FX rates must be represented and reflected in an overall completeness indicator. Do not substitute zero or a fabricated exchange rate, or present a partial sum as a complete valuation.
- **FX dates:** Frankfurter rates are daily; their effective date may differ from the valuation time. The result must retain the rate date and must not imply intraday FX accuracy.
- **Current data contracts:** MDS quote and checkpoint DTOs do not currently include a currency. The assumed initial EUR quote currency must be explicit in the valuation-related contract. FX data will require persistence and a Liquibase migration in MDS.
- **Batching:** Request distinct symbols and currency pairs in batches. Avoid making an MDS or FX call per asset.
- **No write side effects:** Valuation must not mutate assets, `averagePrice`, or portfolio state.
- **No public MDS exposure:** The frontend continues to call Portfolio, not MDS or the Frankfurter API.

## Possible Options

### Option: Use only recent quotes

#### Approach

Calculate an asset's value only when a recent MDS quote is available. Convert it to the portfolio currency and mark the valuation incomplete if a quote or required conversion is missing.

#### Expected Changes

Extend Portfolio's valuation operation and response DTOs. Extend MDS and `MarketDataClient` contracts to include quote currency and batch reads. No asset persistence change is needed.

#### Advantages

- Values are based only on market observations.
- Keeps acquisition cost separate from market price.

#### Disadvantages

- During initial MDS bootstrap, an asset may have no value until its first quote is available.
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
- Conflicts with the agreed restriction that this estimate is allowed only during initial bootstrap.

### Option: Prefer recent quote, then checkpoint, then bootstrap-only estimate — selected

#### Approach

For each symbol, use a recent MDS/Redis quote if available; otherwise use the latest persisted checkpoint with its capture time. Only if neither exists during initial bootstrap may Portfolio use `quantity × averagePrice` as an explicitly labelled estimate. Outside that bootstrap condition, leave the asset value absent. Convert available values to the portfolio's base currency and mark the result incomplete if required data is missing.

#### Expected Changes

Extend MDS observation contracts and reads to support quote currency and checkpoint fallback. Add MDS FX rate ingestion, persistence, and internal batch reads. Extend `MarketDataClient` and Portfolio's valuation orchestration and response DTOs. Do not change `averagePrice` semantics or update behavior.

#### Advantages

- Distinguishes recent quotes, stale observations, and cost-based estimates.
- Supports initial bootstrap without presenting an estimate as a market quote.
- Keeps MDS responsible for market observations and rates, while Portfolio remains responsible for authorization and valuation composition.

#### Disadvantages

- Requires a reliable definition of initial bootstrap and response metadata for the different value sources.
- Requires persisted FX rates and coordinated internal contract changes.
- A valuation may be incomplete when required market or FX data is unavailable; clients must handle that explicitly.

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

Use the selected quote → checkpoint → bootstrap-only estimate flow, with MDS owning market observations and FX reference rates. Portfolio remains the authenticated boundary that composes the valuation and requests needed observations and rates in batches.

Prefer a recent quote, then the latest persisted checkpoint with its actual capture time. Use `quantity × averagePrice` only when no market observation exists during initial bootstrap, and identify it as an estimate. Otherwise, leave the asset value absent. Convert market prices and acquisition costs from their respective source currencies to `PortfolioEntity.baseCurrency`, using persisted Frankfurter rates and their effective dates.

Missing required market or FX data makes the valuation incomplete. Do not fabricate rates or present an incomplete total as complete. `AssetEntity.averagePrice` is not modified.

This fits the existing Portfolio → MDS client and service-authentication flow, keeps data ownership aligned with the existing service boundaries, and avoids exposing provider details or maintaining a second FX cache. The accepted trade-off is a richer response and an explicitly incomplete valuation when necessary, rather than a misleading total.

### Detailed Design

#### Existing Classes to Modify

**`PortfolioMarketDataApi`**  
Path: `services/portfolio-service/src/main/java/com/tychewealth/controller/PortfolioMarketDataApi.java`

Currently defines authenticated latest-price and history operations. Add a public valuation operation at the Portfolio boundary, retaining the existing authenticated user and selection model. The exact route and JSON contract remain to be decided. Do not expose an MDS endpoint directly.

**`PortfolioMarketDataApiController`**  
Path: `services/portfolio-service/src/main/java/com/tychewealth/controller/impl/PortfolioMarketDataApiController.java`

Currently implements the HTTP entry point for Portfolio market-data reads. Delegate the new operation to the Portfolio service, preserving the project's thin-controller convention.

**`PortfolioMarketDataService` and `PortfolioMarketDataServiceImpl`**  
Paths: `services/portfolio-service/src/main/java/com/tychewealth/service/PortfolioMarketDataService.java`, `services/portfolio-service/src/main/java/com/tychewealth/service/impl/PortfolioMarketDataServiceImpl.java`

The implementation currently validates selections, resolves active assets owned by the user, deduplicates symbols, and queries MDS for latest prices or history. Extend this orchestration to load each selected portfolio's `baseCurrency`, retrieve observations and the required distinct FX pairs in batches, calculate per-asset values and the portfolio total, and determine completeness. Preserve existing ownership checks and service-token flow. Do not write asset data.

**`PortfolioMarketDataHelper` and valuation response DTOs**  
Paths: `services/portfolio-service/src/main/java/com/tychewealth/service/helper/marketdata/PortfolioMarketDataHelper.java`, `services/portfolio-service/src/main/java/com/tychewealth/dto/marketdata/`

The helper currently adapts MDS price results and expands them to assets. Extend it, or add a narrowly scoped helper if needed, for valuation mapping and calculation. The response should distinguish a recent quote, checkpoint fallback, bootstrap estimate, and missing value; retain observation and FX effective dates; and indicate whether the valuation is complete. Exact DTO field names and how an incomplete total is represented remain contract details to settle.

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

Current quotes and checkpoints contain a price and capture time but no currency. Recent quotes are read from Redis; checkpoints are persisted in PostgreSQL. Make the initial EUR quote unit explicit in the internal response and provide the latest persisted observation when no recent Redis quote is available. Preserve the observation's real capture time and distinguish it from a recent quote. Do not alter the established six-hour history/checkpoint behavior.

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

1. The authenticated user calls Portfolio's valuation operation using the existing portfolio/asset selection model.
2. Portfolio validates the request and resolves only active assets belonging to that user, including each containing portfolio's `baseCurrency`.
3. Portfolio requests the selected symbols' latest market observations from MDS in one batch. Where a recent Redis quote is unavailable, MDS returns the latest persisted checkpoint if one exists, with its actual capture time.
4. If neither a quote nor checkpoint exists, Portfolio uses `quantity × averagePrice` only when the agreed bootstrap condition is satisfied and labels the result as an estimate. Otherwise, the asset's market value is absent.
5. Portfolio collects the distinct source/target currency pairs for available quote values and estimates, then requests the necessary FX rates from MDS in one batch. Same-currency conversion needs no external rate. Other conversions use the returned rate and effective date; missing rates are never treated as 1:1.
6. Portfolio calculates per-asset values and the portfolio total in the base currency. Missing required prices or rates make the valuation incomplete; the response must not present a partial total as a complete one. No asset is updated.
7. Portfolio returns the result through its authenticated API with enough source, timestamp, FX-date, and completeness information to distinguish quotes, checkpoints, estimates, and missing data.

#### Implementation Outline

1. Settle the public valuation route and response schema, including per-asset source and completeness representation.
2. Define how MDS reports a recent quote versus the last persisted checkpoint, and how the initial-bootstrap condition is determined.
3. Make the initial EUR quote currency explicit and add MDS's Frankfurter daily batch ingestion, rate persistence, internal batch read, Liquibase migration, and tests.
4. Extend `MarketDataClient` with authenticated batch reads for observations and rates.
5. Add Portfolio valuation orchestration and batched currency conversion, with explicit handling of incomplete results and no mutation of `averagePrice`.
6. Test ownership and active-asset filtering; multiple currencies; quote, checkpoint, estimate, and missing-data paths; FX effective dates and missing rates; same-currency conversion; and total/completeness behavior. Update generated API documentation if the public contract changes.

## Open Questions

1. **Exact route and JSON contract:** What public valuation path and stable response fields should be used? How should absent values be represented, and should an incomplete response include a partial subtotal? This matters for API compatibility and frontend behavior; settle it before implementation.
2. **Bootstrap identification:** How will MDS identify that no market observation exists because initial data capture has not yet occurred, rather than because Redis expired or a provider is inactive or failing? This is needed to prevent estimates from concealing operational failures.
3. **Frankfurter coverage and provider mode:** Does the selected Frankfurter mode/source cover every `CurrencyCodeEnum` value supported by the product? Verify coverage, choose the provider policy, and define behavior for unsupported currencies before implementation. Until then, missing rates mean an incomplete valuation—never a 1:1 fallback.

# Spike: Market Price Checkpoint Query Strategy

Spanish version: [market-price-checkpoint-query-spike.es.md](market-price-checkpoint-query-spike.es.md).

## Investigation

This spike decides how the Portfolio history endpoint should return historical prices for charts without exposing pagination to the frontend, while preserving each asset's creation boundary and never presenting an incomplete response as complete. It supersedes the historical-price range/granularity question left open in [`market-data-price-history-spike.en.md`](../market-data-price-history/market-data-price-history-spike.en.md); it does not reopen that spike's other market-data decisions.

The relevant flow already exists:

- `PortfolioMarketDataApi.getPriceHistory` exposes `GET .../market-data/history` with optional `portfolioIds` or `assetIds`, `from`, and `to`. The response is a `List<AssetPriceCheckpointDto>`; the public contract has no pagination parameters.
- `PortfolioMarketDataServiceImpl.getPriceHistory` resolves only the authenticated user's active assets, computes the effective start, calls MDS once with `offset=0` and `limit=maxResults`, validates the response, counts the expanded asset rows, then maps the list. `app.market-data.max-results` defaults to 5,000.
- `PortfolioMarketDataHelper.effectiveHistoryFrom` clamps an omitted or earlier `from` to the oldest selected asset's creation time. `countPriceHistoryResults` and `mapPriceHistory` expand each symbol checkpoint to matching assets and reject/filter checkpoints whose `capturedAt` precedes that individual asset's creation time (or the requested `from`, if later). Assets sharing a symbol can therefore have distinct eligible histories.
- `PortfolioMarketDataValidationHelper.validateHistoryCount` detects an incomplete MDS page (`totalCount > content.size()`), and `validateExpandedHistoryCount` rejects an expanded result above the configured limit with the existing `422` result-limit error. The implementation does not return a truncated prefix as a successful response.
- MDS's `MarketPriceApi` exposes a paginated history route. `MarketPriceApiController` returns `X-Total-Count` and pagination links; `MarketPriceReadServiceImpl` delegates to `MarketPriceCheckpointService.findCheckpoints`, and `MarketPriceReadHelper` builds the filtered, ordered page. Persisted checkpoints are captured every six hours; the history filters currently use inclusive `capturedAt` bounds.
- The internal `MarketDataClient.getPriceHistory` already accepts `from`, `to`, `offset`, and `limit`, and reads the MDS total-count header. Thus MDS can remain paginated for internal/raw consumers while Portfolio presents a complete chart-oriented response.

Portfolio permits selecting up to five portfolios (`MAX_PORTFOLIOS_PER_USER`) or up to 200 assets directly (`MAX_ASSETS_PER_PORTFOLIO`); each portfolio can contain up to 200 assets. The practical chart workload discussed is much smaller—around 20 assets per portfolio, or roughly 100 assets across five portfolios—but the configured 5,000-row cap remains necessary for unusually large selections or long histories.

The chart's normal ranges are one day, one week, one month, six months, one year, and maximum available history. Long ranges do not need every six-hour checkpoint. “Maximum” means all available history for the selected assets, beginning no earlier than their creation; it is not a fixed number of years. A sparse chart series should use actual checkpoints rather than averages or fabricated/interpolated prices.

## Problems and Constraints

- Returning all six-hour checkpoints for long ranges increases response size without adding useful chart detail. With many assets, a shared symbol checkpoint expands into one response row per asset.
- MDS pagination is currently an internal persistence/read mechanism, not the frontend's desired contract. Portfolio asks for only the first page today and correctly returns `422` if that page is incomplete. It must not simply drop that check or treat the first page as the full series.
- The 5,000 limit applies both to MDS checkpoint results and to Portfolio's expanded asset rows. Downsampling needs to happen before Portfolio's expanded response limit is evaluated; that limit remains a final safety guard, not a reason to return partial data.
- MDS queries are symbol-based, while creation times belong to individual Portfolio assets. Portfolio must continue filtering every returned checkpoint against each asset's own `createdAt`; a symbol-level lower bound alone is not sufficient to authorize a point for every asset sharing that symbol.
- The selected representative for an interval must be a real checkpoint. Do not average prices or generate a point at an artificial bucket boundary.
- Keep the existing public response shape and authenticated asset/portfolio ownership checks. Do not expose MDS directly or add public pagination for a frontend that consumes the full selected series.
- Do not impose an arbitrary maximum date range. Older requested dates cannot create prices before an asset existed; Portfolio clamps the lower bound and applies the per-asset filter.
- The global 5,000-row limit remains relevant: at roughly 100 assets, quarterly sampling is about 400 rows/year, but longer histories can still reach the cap. At repository maximums (up to five portfolios and 200 assets each), the cap can be reached sooner. In either case the result must fail explicitly rather than be silently truncated.

## Possible Options

### Option: Require bounded dates and retain the current hard result limit

#### Approach

Require clients to send `from` and `to`, optionally reject ranges beyond a configured duration, and retain Portfolio's existing 5,000-row failure behavior. MDS would continue returning six-hour checkpoints.

#### Expected Changes

Change `PortfolioMarketDataApi` request validation and potentially add a maximum-range configuration and typed validation error. The MDS and `MarketDataClient` paging contracts could remain unchanged.

#### Advantages

- Smallest change to the current flow.
- Continues to use the existing explicit error when the selected range exceeds the result limit.

#### Disadvantages

- Normal chart selections such as one year or maximum history can still fail despite only needing a sparse series.
- A temporal limit does not guarantee fewer than 5,000 rows because results expand per asset.
- Conflicts with the agreed “maximum available history” behavior and would make clients choose artificial date ranges.

### Option: Public date-window or row pagination

#### Approach

Expose consecutive date windows or pages of expanded Portfolio rows, and let the caller combine them. MDS's existing offset/limit pagination could continue to serve its own history API.

#### Expected Changes

Change the Portfolio API and response/request contract, frontend loading/composition, and likely the internal client orchestration. Row pagination would need a stable ordering/cursor for rows after expansion; date windows would need careful inclusive/exclusive boundary semantics.

#### Advantages

- Can retrieve arbitrarily large histories without one large response.
- MDS already has paginated reads for raw checkpoints.

#### Disadvantages

- The frontend explicitly does not need pagination, and assembling windows/pages complicates chart loading.
- MDS pages count symbol checkpoints, whereas Portfolio pages would need to count asset-expanded rows. Different asset creation boundaries make them non-interchangeable.
- Adds contract and ordering complexity without solving the chart's unnecessary six-hour density.

### Option: Resolution-aware downsampling with one complete Portfolio response — selected

#### Approach

Choose a sampling interval from the requested history span. For each interval, retain the last actual checkpoint, then let Portfolio preserve its existing asset-specific creation-date filtering and return the full selected result in the existing response shape. Keep the 5,000-row limit as a final explicit failure guard. “Maximum” uses quarterly points when the available/requested span exceeds two years and has no fixed temporal cap.

#### Expected Changes

Add resolution selection and query-time sampling to the MDS-to-Portfolio history flow. The public Portfolio API remains a single list response. The internal MDS history API remains paginated for consumers that need raw checkpoints; the chart-oriented Portfolio call requests the selected resolution. No schema change or new persistence model is needed: sampling is over stored six-hour checkpoints.

#### Advantages

- Matches the chart's actual information need and keeps its API simple for the frontend.
- Avoids returning six-hour points for long ranges and usually keeps responses well below the configured cap.
- Preserves MDS as the owner of market checkpoints and Portfolio as the owner of asset authorization, composition, and creation boundaries.
- Leaves raw paginated MDS reads available to other internal consumers.
- Uses real prices and does not require a retention policy or arbitrary maximum date.

#### Disadvantages

- Adds interval-selection and sampling behavior to the history read path, which needs deterministic tests at range and bucket boundaries.
- A selected checkpoint that predates a later-created asset must still be filtered out by Portfolio. That asset may consequently have no point for that interval; never return a pre-creation point to fill it.
- The 5,000-row guard can still reject exceptional selections or histories, by design.

## Chosen Solution

### Decision

Choose resolution-aware downsampling for the chart-oriented Portfolio history query, returning one complete, non-paginated response when the selected result is within the configured limit. Keep explicit `422` failure when the MDS page is incomplete or the final asset-expanded result exceeds 5,000 rows; never silently truncate.

Use this initial resolution policy, selected by the requested/available history span:

| History span | Resolution |
| --- | --- |
| Up to 1 week | Existing six-hour checkpoints |
| More than 1 week and up to 1 month | One point every 3 days |
| More than 1 month and up to 6 months | Weekly |
| More than 6 months and up to 2 years | Monthly |
| More than 2 years, including maximum available history | Quarterly |

For each interval, use the last real checkpoint in that interval. Do not average, interpolate, or synthesize prices. “Maximum” starts from the earliest selected asset's creation date and does not have a fixed maximum age. Portfolio continues to apply each asset's individual creation boundary after receiving the sampled checkpoints.

The expected normal workload of around 100 assets produces about 400 rows per year at quarterly resolution. This is a sizing expectation, not a new API limit; the 5,000-row guard remains because the repository permits larger selections and histories.

This option is preferred over bounded date ranges because the product needs maximum history and span alone does not bound expanded rows. It is preferred over pagination because the frontend wants one chart series and MDS's checkpoint pages do not correspond to Portfolio's asset-expanded rows. It is a proportional extension of the existing split: MDS reads/samples market checkpoints; Portfolio authorizes and composes asset rows.

### Detailed Design

#### Existing Classes to Modify

**`PortfolioMarketDataApi`**  
Path: `services/portfolio-service/src/main/java/com/tychewealth/controller/PortfolioMarketDataApi.java`

Currently defines the authenticated frontend history request with optional `from`/`to` and a list response. Keep that public response non-paginated. The date-span policy may require clarifying whether the selected range is supplied explicitly or represented by an existing preset; do not add public page parameters.

**`PortfolioMarketDataServiceImpl`**  
Path: `services/portfolio-service/src/main/java/com/tychewealth/service/impl/PortfolioMarketDataServiceImpl.java`

Currently resolves authorized assets, clamps `from`, calls `MarketDataClient` with offset zero and the configured maximum, then validates completeness and expanded count. It will determine/pass the requested resolution (or delegate selection to the existing helper), call the resolution-aware internal history read, and retain both completeness and expanded-result checks. Ownership checks and token acquisition remain unchanged.

**`PortfolioMarketDataHelper`**  
Path: `services/portfolio-service/src/main/java/com/tychewealth/service/helper/marketdata/PortfolioMarketDataHelper.java`

Currently calculates the effective start, expands symbol checkpoints to assets, and filters with `checkpointAppliesToAsset` using the asset's `createdAt`. Retain these responsibilities and ensure sampled points still pass the same per-asset filter. The response uses the real `capturedAt` of the selected checkpoint, not the interval boundary.

**`MarketDataClient`**  
Path: `clients/market-data-client/src/main/java/com/tychewealth/marketdataclient/MarketDataClient.java`

Currently calls the internal paginated `/internal/prices/history` endpoint with inclusive time filters and `offset`/`limit`. Extend the internal request only as needed to convey the selected resolution for Portfolio's chart query. Keep the existing raw-pagination capability available to other callers; do not make the public Portfolio endpoint inherit MDS pagination semantics.

**`MarketPriceApi` and `MarketPriceApiController`**  
Paths: `services/market-data-service/src/main/java/com/tychewealth/controller/MarketPriceApi.java`, `services/market-data-service/src/main/java/com/tychewealth/controller/impl/MarketPriceApiController.java`

Currently define and return paginated six-hour checkpoint reads, including pagination headers. Add/route an internal resolution-aware history mode for the Portfolio use case while retaining the existing paginated raw read behavior. The controller must continue reporting the total for the actual returned query contract so Portfolio can detect incompleteness; response metadata must not imply the sampled result is a raw-checkpoint page.

**`MarketPriceReadServiceImpl` and `MarketPriceReadHelper`**  
Paths: `services/market-data-service/src/main/java/com/tychewealth/service/impl/MarketPriceReadServiceImpl.java`, `services/market-data-service/src/main/java/com/tychewealth/service/helper/MarketPriceReadHelper.java`

The service currently executes a paginated repository query and maps entities. Add query-time selection of the last checkpoint per requested interval for the chart-oriented mode, preserving the existing filter validation, symbol selection, ordering, and raw paginated behavior. Sampling must happen before page limiting: rank/group matching checkpoints by symbol and UTC interval, retain the last real checkpoint in each group, then apply pagination and report the sampled-result count. Sampling only the first raw checkpoint page would still omit data and be rejected by Portfolio. Sampling should be deterministic in UTC, consistent with persisted UTC bucket conventions.

**`PortfolioMarketDataValidationHelper`**  
Path: `services/portfolio-service/src/main/java/com/tychewealth/service/helper/marketdata/PortfolioMarketDataValidationHelper.java`

Retain validation that an MDS response is complete and that the expanded Portfolio result is at most `app.market-data.max-results`. Do not convert either condition into successful partial output. Update tests/error details only if the sampled query changes what `totalCount` represents.

#### New Classes to Create

No new class or interface is required by the selected design. The resolution mapping and query-time sampling should initially live in the existing history orchestration/read path. Introduce a dedicated abstraction only if implementation demonstrates that this behavior is shared by multiple distinct callers or becomes difficult to test in its existing owner.

#### Component Interaction and Flow

1. `PortfolioMarketDataApi` receives the user's selected assets/portfolios and optional date bounds; public pagination is not introduced.
2. `PortfolioMarketDataServiceImpl` validates the request and resolves only active assets owned by the authenticated user.
3. Portfolio computes the effective start, clamped to the oldest selected asset's creation date, and selects resolution from the history span.
4. `MarketDataClient` calls the internal MDS history route with symbols, time bounds, resolution, and the existing internal paging parameters/authentication.
5. `MarketPriceReadServiceImpl` and `MarketPriceReadHelper` select the last actual checkpoint in each interval for the resolution-aware chart query. The existing raw MDS paginated read remains available for consumers needing checkpoints rather than chart sampling.
6. Portfolio verifies the MDS response is complete, applies the per-asset `createdAt` and requested-`from` boundary, and expands each sampled symbol checkpoint into rows for the corresponding assets.
7. Portfolio returns the complete list if it is within `app.market-data.max-results`; otherwise the existing typed `422` error is returned. It never returns a truncated list as success.

#### Implementation Outline

1. Define the exact resolution selector from requested range and the UTC interval/bucket semantics, including open-ended `to` and “maximum” history.
2. Extend the internal MDS/`MarketDataClient` contract for resolution-aware history without removing raw pagination.
3. Implement deterministic last-real-checkpoint-per-interval selection in the existing MDS read path.
4. Keep Portfolio's asset ownership, effective-start, per-asset creation filter, expanded-count check, and response DTO unchanged unless tests expose a necessary contract adjustment.
5. Add service/client/controller and Portfolio integration tests for every resolution tier, range boundaries, assets sharing a symbol with different creation dates, checkpoints before creation, absent/empty history, MDS incomplete pages, and the 5,000-row error.
6. Update generated API documentation if the public request contract changes; no persistence migration is expected.

## Open Questions

No product-level decision remains open: the response is complete and non-paginated, long periods are downsampled, maximum history is unbounded by date, and creation boundaries and explicit result-limit errors remain. Implementation must settle exact UTC bucket anchoring and how the internal resolution is represented in the MDS request; these are contract details for the implementation story, not unresolved architectural choices.


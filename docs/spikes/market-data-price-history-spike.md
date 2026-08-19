# Spike: Market Price History

## Investigation

The repository is a Maven reactor that includes `portfolio-service` and `market-data-service` (MDS). MDS is already prepared to run scheduled work through `@EnableScheduling` and load properties through `@ConfigurationPropertiesScan` in `TycheWealthMarketDataServiceApplication`. Its configuration already includes Redis, the `active-symbol-changes` topic, and Twelve Data properties; `TwelveDataApiClient` is an active external client. However, the proposed initial quote source for this capability is a dedicated market mock, not the legacy vehicle surface. All `Vehicle*` code and configuration in MDS are legacy migration/deletion debt, must not be reused for the new market data design, and are expected to be removed over time.

Currently, Portfolio's `ActiveSymbolService.synchronizeSymbols` compares the current symbol set with a Redis snapshot and publishes the delta through `ActiveSymbolChangesEventPublisher`. The `ActiveSymbolChanges` event carries only added and removed symbol sets, and the identically named MDS consumer only logs it. MDS's `KafkaConfig` already defines retries and a DLT. The publisher uses `kafkaTemplate.send(topic, event)` without a key; therefore, an event identifier alone does not establish ordering.

MDS does not yet have entities, repositories, or a Liquibase changelog. Its parent already provides JPA and Liquibase, and the MDS POM provides WebFlux. Portfolio does persist assets and portfolios. In particular, `AssetEntity.averagePrice` represents the average acquisition cost, not a market quote; an MDS price read must not modify it.

The frontend response must enter through a new Portfolio endpoint, not MDS. Portfolio obtains symbols from authenticated portfolios in its own database, deduplicates them, and synchronously queries MDS. MDS will be an internal dependency and must not be publicly exposed. Service-to-service authentication is deliberately deferred.

## Problems and Constraints

- A Redis cache alone cannot provide a historical chart or retain data after expiration. A latest price and persistent checkpoints are required.
- Capture must be independent of process memory. A six-hour counter resets on redeploy and is not a reliable time boundary.
- The MDS scheduler must be able to obtain a quote when the Redis entry is missing; it cannot assume the cache was always pre-warmed.
- Checkpoints are retained indefinitely and must be idempotent for a symbol and time bucket.
- The effective per-asset symbol limit already defined in the Portfolio database must apply to the aggregate query. MDS must defensively validate the same limit without introducing a new numeric value.
- Kafka deltas can arrive duplicated or without ordering sufficient for distributed state evolution. Retries and a DLT do not replace idempotency or ordering.
- Current physical deletion of assets and portfolios would prevent preserving the relationship required for future position reconstruction. `PortfolioEntity` uses `CascadeType.ALL` and `orphanRemoval`, and current services delete through repositories.
- Asset change history must be audited automatically within the mutation transaction. It is not a transactions table or ledger: an actual transaction microservice is out of scope.
- MDS read routes and DTOs have not been decided. Redis or tables must not become implementation endpoints.

## Possible Options

### Option 1: Redis as the Only Price Store

#### Approach

Store only the latest JSON quote per symbol in Redis and serve it directly.

#### Expected Changes

Add a quote provider and Redis write/read operations in MDS.

#### Advantages

- It is the lowest initial-cost alternative with fast reads.
- It requires no schema or database migrations.

#### Disadvantages

- Expiration removes history and cannot support price charts.
- It provides no durable basis for later historical calculations.

### Option 2: Redis Cache Plus MDS Historical Store

#### Approach

Keep the latest JSON quote in Redis and persist checkpoints per symbol and six-hour bucket in PostgreSQL.

#### Expected Changes

Add a model, repositories, Liquibase, delta consumption, a five-minute scheduler, and internal read endpoints to MDS.

#### Advantages

- It separates low-latency latest-price access from historical retention.
- It supports per-symbol charts without updating the asset acquisition cost.
- Checkpoint uniqueness makes capture restartable and idempotent.

#### Disadvantages

- It introduces persistence and schema maintenance to a service that does not yet have them.
- It requires Kafka contract evolution and data operation design.

### Option 3: Six-Hour Counter or Two Schedulers

#### Approach

Use an in-memory counter to decide when to save, or separate a quote job every five minutes from another job every six hours.

#### Expected Changes

Add operational state or coordinate two schedulers and their partial failures.

#### Advantages

- The time intent appears explicit at first glance.

#### Disadvantages

- The counter is lost on restart and can duplicate or omit captures.
- Two schedulers increase coordination and do not themselves solve idempotency.

### Option 4: Portfolio Scheduler or Composition on Request

#### Approach

Have Portfolio query and persist quotes periodically, or have the frontend call MDS directly.

#### Expected Changes

Create a Portfolio scheduler or expose MDS to the client.

#### Advantages

- A Portfolio scheduler appears close to user assets.
- The direct call initially removes one HTTP hop.

#### Disadvantages

- It mixes market ownership with portfolio ownership and duplicates symbols across users.
- A Portfolio scheduler does not provide a shared price source; none is proposed.
- Exposing MDS breaks the service boundary and delegates authorization and composition to the client.

### Option 5: Overwrite `averagePrice` with the Current Quote

#### Approach

Update `AssetEntity.averagePrice` whenever a market price is obtained.

#### Expected Changes

Connect price reads to Portfolio asset writes.

#### Advantages

- It reuses an existing column.

#### Disadvantages

- It destroys the meaning of average acquisition cost.
- It makes a read have write effects and confuses performance with market valuation.

### Option 6: External CRUD Audit or Immutable Automatic Entries

#### Approach

Offer generic CRUD for audit records, or create entries as part of asset mutations.

#### Expected Changes

The first alternative requires history write endpoints; the second adds an entity and internal transactional persistence.

#### Advantages

- Generic CRUD appears reusable for external clients.
- Automatic entries retain the exact mutation context.

#### Disadvantages

- CRUD allows history to be rewritten or deleted and does not ensure it accompanies the asset change.
- Automatic auditing adds work to create, update, and delete flows.

### Option 7: Physical Deletion or Soft Deletion

#### Approach

Keep the current `delete` operations, or add `deleted_at` to portfolios and assets and explicitly filter active entities.

#### Expected Changes

Soft deletion requires migrations, repository filters, validations, listings, retrievals, counts, conflict checks, and changes to `AssetRepository.findDistinctSymbolsByUserIds`.

#### Advantages

- Physical deletion is simpler in the short term.
- Soft deletion retains assets and their changes for future reconstruction.

#### Disadvantages

- Physical deletion removes the link required by history.
- Soft deletion requires discipline in every query; the `UserEntity.deletedAt` precedent uses explicit filters and does not imply an automatic global filter.

### Option 8: Direct Twelve Data or a Dedicated Market Mock

#### Approach

Query `TwelveDataApiClient` immediately, or first introduce a dedicated market mock behind the quote capability.

#### Expected Changes

The direct call requires configuring and managing the real provider from the first increment; the mock requires a dedicated market component and must not reuse any `Vehicle*` class or configuration.

#### Advantages

- Twelve Data provides real external prices.
- The mock enables flow development and testing without quota, credentials, or external availability dependencies.

#### Disadvantages

- Twelve Data adds operational cost and external variability before the flow is stable.
- The mock does not represent production prices and must be replaced later.

## Chosen Solution

### Decision

Adopt the Redis cache plus MDS historical store option, with a dedicated market mock provider as the initial implementation. MDS will own the active symbol catalog, recent quotes, and checkpoints; Portfolio will continue to own portfolios, assets, and their change history. Portfolio will compose the authenticated frontend response through a concrete Spring `RestClient`-based `MarketDataClient`, without introducing an interface solely for that call.

The MDS scheduler will run every five minutes. For each active symbol, it obtains a provider quote, stores it as JSON in Redis under a per-symbol key with a ten-minute TTL, and persists it if the current six-hour UTC bucket has no checkpoint. The bucket boundary is derived from time, not a counter. If Redis does not contain a quote, the scheduler must request it itself.

The interim `active-symbol-changes` contract will add `eventId` and `occurredAt`. MDS will consume idempotently. While the contract remains delta-only, the topic will initially operate with one partition. This limits, but does not conceptually eliminate, the need for evolution: when volume or ordering requirements scale, the contract must include a snapshot version. An isolated `eventId` is not an ordering guarantee.

### Detailed Design

MDS will persist `market_symbols` with `id`, a unique normalized symbol, `active`, `created_at`, `updated_at`, and `deactivated_at`. Activation and deactivation will be the idempotent projection of the Kafka delta. `market_price_checkpoints` will contain `id`, `market_symbol_id` as an FK, `price`, `captured_at`, and `bucket_start`, with uniqueness on `(market_symbol_id, bucket_start)`. Checkpoints have no expiration policy.

The dedicated market mock provider will deliver quotes to the polling flow. `TwelveDataApiClient` remains an existing external client and candidate for a later evolution. All MDS `Vehicle*` classes and configuration are legacy migration/deletion debt, do not participate in this design, must not be reused, and are expected to be removed over time.

Portfolio will add a new endpoint for prices and charts for authorized symbols. It will resolve symbols from its active database assets, deduplicate them, and synchronously call MDS internal endpoints for the latest price and history. Concrete routes and DTOs are deferred; they will not be endpoints that literally expose Redis or tables. MDS will defensively validate the same effective maximum symbol capacity that Portfolio enforces in its database, without setting a different number here. Inter-service security will be decided before exposing the integration outside the internal network.

To support future portfolio valuation history, Portfolio will add `asset_changes`: `id`, `asset_id` as its only FK, `change_type` (`CREATED`, `MANUAL_UPDATE`, `DELETED`), `previous_quantity`, `new_quantity`, `previous_average_price`, `new_average_price`, and `occurred_at`. The four quantity/price values can be null depending on the change type. It will not carry `portfolio_id`: the asset already identifies the portfolio and soft deletion retains that relationship. There will be no external POST, UPDATE, or DELETE for these records. Asset PATCH will create an automatic immutable entry in the same transaction only when relevant fields change.

Portfolio will adopt soft deletion with `deleted_at` on `portfolios` and `assets`. Deleting a portfolio will mark its assets and create their deletion audit entries. The current `uq_asset_portfolio_id_symbol` constraint must migrate to a PostgreSQL partial unique index on `(portfolio_id, symbol) WHERE deleted_at IS NULL`, allowing a previously deleted symbol to be recreated. This preserves active-asset semantics without inventing global JPA behavior.

### Existing Classes to Modify

- `TycheWealthMarketDataServiceApplication`: retain its scheduling and property-scan configuration to register the new components.
- `ActiveSymbolChangesEventConsumer` and MDS `ActiveSymbolChanges`: apply the new contract, idempotent projection, and symbol activation/deactivation.
- MDS `KafkaConfig`: retain retries and DLT while adapting deserialization/consumption to the evolved contract.
- Portfolio `ActiveSymbolService`, `ActiveSymbolChanges`, and `ActiveSymbolChangesEventPublisher`: emit `eventId` and `occurredAt`; the current publisher does not use a key.
- `AssetEntity`, `PortfolioEntity`, `AssetRepository`, `PortfolioRepository`, validators, and asset/portfolio services: implement `deleted_at`, explicit active-entity filters, and soft deletion. `AssetRepository.findDistinctSymbolsByUserIds` must also exclude deleted assets and portfolios.
- `AssetServiceImpl`: create immutable changes during PATCH and deletion, without altering `averagePrice` for market reads.
- `PortfolioServiceImpl`: replace physical deletion and mark associated assets when deleting a portfolio.
- The Liquibase master changelogs of both services: add the schemas and indexes for their respective domains.

### New Classes to Create

- In MDS: `market_symbols` and `market_price_checkpoints` entities, repositories, and services; a dedicated market mock quote provider; a five-minute scheduler; and internal DTOs and read controllers for latest price and history.
- In Portfolio: a concrete Spring `RestClient`-based `MarketDataClient`, the service/controller that composes prices for the authenticated user, the `asset_changes` entity/repository, and the `change_type` type.

Final route, DTO, and orchestration-class names will be decided in implementation stories; the spike defines responsibilities and boundaries, not final public signatures.

### Component Interaction and Flow

1. User service publishes active users; Portfolio consumes them, and `ActiveSymbolService.synchronizeSymbols` compares the Redis snapshot with active symbols from non-deleted portfolios.
2. Portfolio publishes the `addedSymbols`/`removedSymbols` delta with `eventId` and `occurredAt` to `active-symbol-changes`, initially on a one-partition topic.
3. MDS consumes the event idempotently and activates or deactivates `market_symbols` records. Failures follow the existing retry and DLT policy.
4. Every five minutes, the scheduler queries the mock for each active symbol, updates the JSON Redis cache with a ten-minute TTL, and persists a checkpoint only if the six-hour UTC bucket is new.
5. The frontend requests the price view from Portfolio. Portfolio determines symbols from the user's active assets, deduplicates them, and requests the latest price and/or history from MDS.
6. MDS returns market data. Portfolio presents it without writing `AssetEntity.averagePrice`.
7. When a user creates, manually changes, or deletes an asset, Portfolio records the automatic change in `asset_changes`; these entries, together with historical prices, make it possible to later consider historical valuation if holding changes are also reconstructed.

### Implementation Outline

Delivery must be divided by responsibility boundaries: first the MDS model and migrations with idempotent symbol projection and the mock; then cache, scheduler, and checkpoints; then internal reads and composition from Portfolio; and, in parallel or beforehand, Portfolio soft deletion and `asset_changes`, which make future reconstruction viable. Every story that changes persistence will include the entity, repository, Liquibase changelog, and corresponding tests in its service.

This decision does not include implementing a transaction microservice, complete historical portfolio valuation, service-to-service authentication, Twelve Data as the initial provider, or a versioned snapshot contract. These are evolutions contingent on production needs, scale, and ordering.

## Open Questions

- What authentication and authorization mechanism will Portfolio and MDS use before enabling the internal call in a shared environment?
- What symbol normalization policy and market/currency will the mock initially represent?
- How will rate limits, errors, and gradual replacement of the mock by Twelve Data be managed?
- What version and form will the snapshot contract take when one partition and deltas no longer satisfy ordering or recovery requirements?
- What granularity, presentation time zone, and maximum range should the history query support while retaining the UTC bucket as the persistence key?
- Which asset changes, beyond quantity and average cost, will be relevant to reconstruct future valuation?
- How will existing queries be migrated to ensure every read, count, and conflict check excludes only entities with non-null `deleted_at`?

## Stories Derived from the Spike

### Evolve the Active Symbol Kafka Contract

**Objective:** evolve `ActiveSymbolChanges` to carry `eventId` and `occurredAt`, retaining added and removed symbol sets, and initially operate `active-symbol-changes` with one partition.

**Acceptance Criteria:**

- The Portfolio producer publishes every delta with non-null `eventId` and `occurredAt`.
- The contract documents that messages are deltas and that `eventId` does not by itself guarantee ordering.
- Topic operational configuration uses one partition while the consumer relies on ordered deltas.
- The existing MDS retry and DLT policy is retained for the evolved contract.
- The future need for a versioned snapshot contract is documented if scale or recovery/ordering requirements increase.

### Persist Market Symbols and Consume Deltas Idempotently in MDS

**Objective:** add the persistent `market_symbols` catalog to MDS and make the consumer idempotently project active-symbol events.

**Acceptance Criteria:**

- Liquibase creates `market_symbols` with `id`, a unique normalized symbol, `active`, `created_at`, `updated_at`, and `deactivated_at`.
- Repeated events do not produce inconsistent states or duplicate rows.
- An added symbol becomes active, and a removed symbol becomes inactive with its corresponding timestamp.
- The consumer no longer only logs the event and retains retry and DLT configuration.
- The implementation includes entity, repository, changelog, and MDS tests in the same delivery.

### Capture Mock Quotes, Redis Cache, and Persistent Checkpoints

**Objective:** obtain quotes through a dedicated market mock, update the latest price, and retain six-hour checkpoints.

**Acceptance Criteria:**

- An MDS scheduler runs every five minutes and processes only active symbols.
- The market mock is a new capability and does not reuse `VehiclePollingScheduler`, `VehiclePollingService`, any other `Vehicle*` class, or `Vehicle*` configuration; these are legacy migration/deletion debt expected to be removed over time.
- Each quote is stored as JSON in Redis with a per-symbol key and a ten-minute TTL.
- If the cache has no quote, the scheduler queries the provider before continuing.
- Liquibase creates `market_price_checkpoints` with an FK to `market_symbols`, `price`, `captured_at`, `bucket_start`, and uniqueness on `(market_symbol_id, bucket_start)`.
- The bucket is calculated from UTC and no in-memory counter exists; at most one checkpoint per symbol and bucket is retained.
- Checkpoints have no expiration or associated deletion job.

### Expose MDS Internal Reads and Compose Prices from Portfolio

**Objective:** allow Portfolio to deliver recent prices and history for its authorized symbols to the frontend without exposing MDS directly.

**Acceptance Criteria:**

- MDS offers internal endpoints for latest price and history; routes and DTOs are defined in the story without exposing Redis or tables as an API.
- Portfolio adds an authenticated frontend-oriented endpoint and resolves symbols for the user's active assets from its database.
- Portfolio deduplicates symbols and uses a concrete Spring `RestClient`-based `MarketDataClient` to synchronously query MDS.
- Portfolio and MDS apply the same effective Portfolio symbol limit; MDS defensively validates it without introducing a new numeric limit.
- Response composition does not write or modify `AssetEntity.averagePrice`.
- MDS remains an internal service; service-to-service authentication is treated as a pending decision before exposure outside that boundary.

### Record Immutable Asset Change Auditing

**Objective:** retain relevant holding changes in `asset_changes` without introducing audit CRUD or a transaction ledger.

**Acceptance Criteria:**

- Liquibase creates `asset_changes` with `id`, `asset_id` as its only FK, `change_type`, nullable previous/new quantities and average costs, and `occurred_at`.
- `change_type` supports exactly `CREATED`, `MANUAL_UPDATE`, and `DELETED`.
- Asset creation, relevant manual update, and deletion automatically generate an entry in the same transaction as the mutation.
- PATCH records previous and new quantity and average-cost values only when applicable.
- No external endpoints exist to create, update, or delete audit entries.
- The table does not include `portfolio_id` and is not presented as a transactions table or ledger.

### Apply Soft Deletion and Migrate Portfolio Uniqueness and Queries

**Objective:** replace physical deletion of portfolios and assets with soft deletion, preserving the relationship required for history and symbol recreation.

**Acceptance Criteria:**

- Liquibase adds `deleted_at` to `portfolios` and `assets`.
- Asset deletion marks its `deleted_at` and creates its `DELETED` audit entry; it does not physically remove the row.
- Portfolio deletion marks the portfolio and its associated assets and creates applicable deletion audit entries.
- The `uq_asset_portfolio_id_symbol` constraint is replaced with the PostgreSQL partial unique index `(portfolio_id, symbol) WHERE deleted_at IS NULL`.
- A deleted symbol can be recreated in the same portfolio, while duplicate active symbols continue to be rejected.
- Repositories, validations, listings, retrievals, counts, and conflict checks explicitly filter non-deleted entities.
- `AssetRepository.findDistinctSymbolsByUserIds` excludes deleted assets and portfolios so inactive symbols are not published.
- The implementation does not assume an automatic global filter: it follows the explicit-filter precedent used by user soft deletion.

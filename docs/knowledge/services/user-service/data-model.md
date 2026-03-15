# User Service Data Model

This page consolidates the **persistence model** for `user-service`, replacing the older split between detailed table pages, **schema notes**, and entity summaries to provide a centralized reference.

## Overview
This section introduces the purpose of this documentation. It consolidates the persistence details for the `user-service`, including **JPA entities** and schema relationships, by presenting them in a structured manner. This simplifies navigation and reference for developers.

## Entity Summary
This table offers a concise summary of the primary entities involved in the user-service data model, highlighting their roles and interconnections for quick understanding.

| Entity             | Table Name    | Role Description                 | Key Relations                |
|--------------------|---------------|----------------------------------|------------------------------|
| **AssetEntity**    | `assets`      | Manages assets with their details and ownership; referenced by entities requiring asset handling. | Has a `currency` field, and is linked by a `portfolio_id` relation. |
| **PortfolioEntity**| `portfolios`  | Represents investment portfolios with user-assigned settings and holdings; serves as a container for assets. | Has a `name` field for identification, and includes references to owned `assets` and linked `users`. |

*Note: For precise nullability and timestamp details, refer to the actual JPA entities and Liquibase changelog files.*

## Implemented Entities
The following sections detail each entity within the user-service persistence layer.

### AssetEntity
This entity manages asset details, including attributes for user-defined data and standard fields. It supports core functionality for asset tracking and enforcement within the service.

**#### Fields**
- `String symbol`: Defines the asset code.
- `AssetTypeEnum asset_type`: Enumerates the asset type for categorization.
- `BigDecimal quantity`: Holds the amount of the asset owned.
- `BigDecimal average_price`: Tracks the average acquisition price for cost calculations.

<u>Note:</u> For full details on nullability and foreign keys, consult the JPA Entity class and related Liquibase changelogs.

### PortfolioEntity
This entity models investment portfolios, capturing user-chosen parameters and asset allocations. It facilitates portfolio management features throughout the service.

**#### Fields**
- `String name`: Provides a unique identifier for the portfolio.
- `String description`: Optional summary explaining the portfolio's purpose.
- `CurrencyCodeEnum base_currency`: Specifies the primary currency for value calculations.
- `RiskProfileEnum risk_profile`: Defines the portfolio's risk tolerance level.
- `InvestmentHorizonEnum investment_horizon`: Indicates the time frame for investments.
- `List<AssetEntity> assets`: Collection object containing all assets within the portfolio.

<u>Additional:</u> Foreign-key constraints and revocation flags must be referenced in schema change reviews.

### refresh_tokens
This standalone token table ensures secure user sessions through token-based authentication. It operates independently but aligns with security requirements.

**#### Fields**
- `String token`: Unique identifier for the session.
- `Instant expires_at`: Timestamp marking the end of the token's validity.
- `boolean revoked`: Flag indicating if the token has been invalidated.
- `Instant created_at`: Record of when the token was issued.

<u>Important:</u> Token management behavior should be understood through application runtime documentation and database schema files.

### users
This entity forms the foundation of user management, tracking account details and creation metadata. It ties into multiple functionalities via relationships.

**#### Fields**
- `String email`: The registered email address, serving as a primary identifier.
- `String username`: A user-chosen handle for logging or display.
- `String password`: The hashed or encrypted password for security.
- `LocalDateTime created_at`: Date and time the user account was established.
- `LocalDateTime deleted_at`: If present, indicates hard deletion via the `deleted_at` convention.

<vmark>Note: Null checks and soft-deletion logic are enforced through application runtime and database schema definitions.</vmark>

## Relationships
The following diagram illustrates the inter-entity relationships within the user-service data model.
```mermaid
flowchart LR
  classDef entity fill:#1f3d37,stroke:#73e5c6,stroke-width:2px,color:#ffcc6`
  fill:#17312d,stroke:#ffb347,color:#ffcc6;
  assets["<b>assets</b><br/>String symbol<br/>AssetTypeEnum asset_type<br/>BigDecimal quantity<br/>BigDecimal average_price<br/>CurrencyCodeEnum currency<br/>..."]
  portfolios["<b>portfolios</b><br/>String name<br/>String description<br/>CurrencyCodeEnum base_currency<br/>RiskProfileEnum risk_profile<br/>InvestmentHorizonEnum investment_horizon<br/>..."]
  refresh_tokens["<b>refresh_tokens</b><br/>String token<br/>Instant expires_at<br/>boolean revoked<br/>Instant created_at"]

  users["<b>users</b><br/>String email<br/>String username<br/>String password<br/>LocalDateTime created_at<br/>LocalDateTime deleted_at"]
  portfolios -->|portfolio_id| assets
  users -->|user_id| portfolios
  users -->|user_id| refresh_tokens
```

## Persistence and Schema Notes
Maintain the integrity of this documentation by ensuring updates are based on JPA entities, Liquibase changelogs, and schema files. Always cross-reference with these sources before modifying this summary.

- All persistence logic derives from code artifacts, not this text.
- Review **nullability constraints**, **token revocation mechanisms**, **timestamp precision**, and **foreign-key ownership patterns** in code.

## Related Documentation
This section provides links to supplementary materials for a complete understanding:

- `docs/knowledge/services/user-service/overview.md`
- `docs/knowledge/services/user-service/runtime.md`
- `docs/knowledge/database/overview.md`

Ensure you refer to these pages for context-dependent information.

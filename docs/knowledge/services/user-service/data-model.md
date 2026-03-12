# User Service Data Model

## Overview

This page consolidates the persistence model for `user-service`. It replaces the older split between table pages, schema notes, and entity summaries.

## Entity Summary

| Entity | Table | Role | Key Relations |
| --- | --- | --- | --- |
| `AssetEntity` | `assets` | Represents an asset position that belongs to a portfolio. | PortfolioEntity |
| `PortfolioEntity` | `portfolios` | Groups assets and investment preferences owned by a user. | AssetEntity, UserEntity |
| `RefreshTokenEntity` | `refresh_tokens` | Stores refresh tokens, expiry, and revocation state linked to a user. | UserEntity |
| `UserEntity` | `users` | Stores the primary user identity and credential state. | PortfolioEntity |

## Implemented Entities

### `AssetEntity` (`assets`)

- Role: Represents an asset position that belongs to a portfolio.

#### Fields

| Column | Type | Required | Meaning | Notes |
| --- | --- | --- | --- | --- |
| `symbol` | `String` | Yes | Ticker or symbol used to identify the asset. | core identifier |
| `asset_type` | `AssetTypeEnum` | Yes | Classification of the asset instrument. | enum value |
| `quantity` | `BigDecimal` | Yes | Position size currently held in the portfolio. | No special note. |
| `average_price` | `BigDecimal` | Yes | Average acquisition price for the asset position. | No special note. |
| `currency` | `CurrencyCodeEnum` | Yes | Currency associated with the asset valuation. | enum value, currency context |
| `created_at` | `LocalDateTime` | Yes | Timestamp recording when the asset record was created. | timestamp |
| `updated_at` | `LocalDateTime` | No | Timestamp recording the most recent asset update. | timestamp |

#### Relations

- Many records point to `PortfolioEntity` through `portfolio_id`.

### `PortfolioEntity` (`portfolios`)

- Role: Groups assets and investment preferences owned by a user.

#### Fields

| Column | Type | Required | Meaning | Notes |
| --- | --- | --- | --- | --- |
| `name` | `String` | Yes | Short portfolio name shown to the user. | core identifier |
| `description` | `String` | No | Optional free-text description of the portfolio. | No special note. |
| `base_currency` | `CurrencyCodeEnum` | Yes | Reference currency used to express portfolio values. | enum value, currency context |
| `risk_profile` | `RiskProfileEnum` | No | Risk appetite classification linked to the portfolio. | enum value |
| `investment_horizon` | `InvestmentHorizonEnum` | No | Expected holding horizon for the portfolio strategy. | enum value |
| `strategy_type` | `StrategyTypeEnum` | No | Strategy style associated with the portfolio. | enum value |
| `max_risk` | `BigDecimal` | No | Optional cap on accepted portfolio risk. | No special note. |
| `created_at` | `LocalDateTime` | Yes | Timestamp recording when the portfolio was created. | timestamp |
| `updated_at` | `LocalDateTime` | No | Timestamp recording the most recent portfolio update. | timestamp |

#### Relations

- Many records point to `UserEntity` through `user_id`.
- One record owns a collection associated with `AssetEntity`.

### `RefreshTokenEntity` (`refresh_tokens`)

- Role: Stores refresh tokens, expiry, and revocation state linked to a user.

#### Fields

| Column | Type | Required | Meaning | Notes |
| --- | --- | --- | --- | --- |
| `token` | `String` | Yes | Opaque refresh-token value persisted for token rotation. | core identifier |
| `expires_at` | `Instant` | Yes | Instant after which the refresh token is no longer valid. | timestamp |
| `revoked` | `boolean` | Yes | Flag indicating whether the refresh token can still be used. | lifecycle flag |
| `created_at` | `Instant` | Yes | Timestamp recording when the refresh token was issued. | timestamp |

#### Relations

- Many records point to `UserEntity` through `user_id`.

### `UserEntity` (`users`)

- Role: Stores the primary user identity and credential state.

#### Fields

| Column | Type | Required | Meaning | Notes |
| --- | --- | --- | --- | --- |
| `email` | `String` | Yes | Primary email used to identify the user account. | core identifier |
| `username` | `String` | Yes | Public-facing or login-friendly user name. | core identifier |
| `password` | `String` | Yes | Stored password hash used during authentication. | sensitive credential data |
| `created_at` | `LocalDateTime` | Yes | Timestamp recording when the user record was created. | timestamp |

#### Relations

- One record owns a collection associated with `PortfolioEntity`.

## Relationships

```mermaid
%%{init: {
  "theme": "base",
  "themeVariables": {
    "background": "#1c2c29",
    "primaryColor": "#24443d",
    "primaryTextColor": "#ffcc66",
    "primaryBorderColor": "#73e5c6",
    "lineColor": "#ffb347",
    "secondaryColor": "#203934",
    "tertiaryColor": "#294c44",
    "mainBkg": "#24443d",
    "secondBkg": "#203934",
    "tertiaryBkg": "#294c44",
    "clusterBkg": "#17312d",
    "clusterBorder": "#57c8a8",
    "nodeBkg": "#24443d",
    "nodeBorder": "#73e5c6",
    "defaultLinkColor": "#ffb347",
    "titleColor": "#ffcc66",
    "textColor": "#ffcc66",
    "edgeLabelBackground": "#17312d",
    "actorBkg": "#24443d",
    "actorBorder": "#73e5c6",
    "actorTextColor": "#ffcc66",
    "actorLineColor": "#ffb347",
    "signalColor": "#ffb347",
    "signalTextColor": "#ffcc66",
    "labelBoxBkgColor": "#17312d",
    "labelBoxBorderColor": "#57c8a8",
    "labelTextColor": "#ffcc66",
    "loopTextColor": "#ffcc66",
    "noteBkgColor": "#294c44",
    "noteBorderColor": "#82e7cb",
    "noteTextColor": "#ffcc66",
    "activationBkgColor": "#2c5a50",
    "activationBorderColor": "#73e5c6",
    "sectionBkgColor": "#1b3531",
    "altSectionBkgColor": "#22413b",
    "gridColor": "rgba(255, 179, 71, 0.22)"
  },
  "themeCSS": ".messageText, .messageText tspan, .label text, .label tspan, .edgeLabel text, .edgeLabel tspan, .nodeLabel, .nodeLabel p, .nodeLabel span, .label foreignObject, .label foreignObject div, .cluster-label text, .cluster-label tspan, .actor text, .actor tspan, .loopText, .noteText, .er text, .er tspan, .er.entityLabel, .er.attributeText, .er.relationshipLabel, .er foreignObject div, .er foreignObject span, .er foreignObject td, .er foreignObject th { fill: #ffcc66 !important; color: #ffcc66 !important; } .messageLine0, .messageLine1, .flowchart-link, .edgePath path, .actor-line, .er.relationshipLine { stroke: #ffb347 !important; fill: none !important; } .arrowheadPath, marker path { stroke: #ffb347 !important; fill: #ffb347 !important; } .er rect, .er .entityBox, .er .attributeBoxEven, .er .attributeBoxOdd { fill: #24443d !important; stroke: #73e5c6 !important; } .er .relationshipLabelBox { fill: #17312d !important; stroke: #57c8a8 !important; } .er foreignObject, .er foreignObject div, .er foreignObject table { background: #24443d !important; } .er foreignObject tr:nth-child(odd), .er foreignObject tr:nth-child(odd) td { background: #24443d !important; } .er foreignObject tr:nth-child(even), .er foreignObject tr:nth-child(even) td { background: #2b4f47 !important; } .er foreignObject td, .er foreignObject th { border-color: #73e5c6 !important; }"
}}%%
flowchart LR
  classDef entity fill:#1f3d37,stroke:#73e5c6,stroke-width:2px,color:#ffcc66;
  classDef relation fill:#17312d,stroke:#ffb347,color:#ffcc66;
  assets["<b>assets</b><br/>String symbol<br/>AssetTypeEnum asset_type<br/>BigDecimal quantity<br/>BigDecimal average_price<br/>CurrencyCodeEnum currency<br/>..."]
  class assets entity;
  portfolios["<b>portfolios</b><br/>String name<br/>String description<br/>CurrencyCodeEnum base_currency<br/>RiskProfileEnum risk_profile<br/>InvestmentHorizonEnum investment_horizon<br/>..."]
  class portfolios entity;
  refresh_tokens["<b>refresh_tokens</b><br/>String token<br/>Instant expires_at<br/>boolean revoked<br/>Instant created_at"]
  class refresh_tokens entity;
  users["<b>users</b><br/>String email<br/>String username<br/>String password<br/>LocalDateTime created_at"]
  class users entity;
  portfolios -->|portfolio_id| assets
  users -->|user_id| portfolios
  users -->|user_id| refresh_tokens
```

## Persistence and Schema Notes

- Treat JPA entities and schema-management files as the source of truth for persistence details.
- Review nullability, token revocation flags, timestamps, and foreign-key ownership in code before changing this page.
- Liquibase changelogs are present and should be checked together with entities when schema behavior changes.

## Related Documentation

- `docs/knowledge/services/user-service/overview.md`
- `docs/knowledge/services/user-service/runtime.md`
- `docs/knowledge/database/overview.md`

# Database Overview

## Overview

This page consolidates the implemented database model across the repository.

## Snapshot

| Aspect | Current State |
| --- | --- |
| Implemented services with persistence | `1` |
| Total detected entities | `4` |
| Liquibase-backed services | `1` |

## Implemented Service Schemas

## user-service

### Snapshot

| Aspect | Current State |
| --- | --- |
| Service | `user-service` |
| Detected entities | `4` |
| Tables represented | `4` |
| Many-to-one relations | `3` |
| Liquibase changelogs | Present |

### Implemented Entities

| Table | Entity | Role | Key Links |
| --- | --- | --- | --- |
| `assets` | `AssetEntity` | Represents an asset position that belongs to a portfolio. | Portfolio |
| `portfolios` | `PortfolioEntity` | Groups assets and investment preferences owned by a user. | Asset, User |
| `refresh_tokens` | `RefreshTokenEntity` | Stores refresh tokens, expiry, and revocation state linked to a user. | User |
| `users` | `UserEntity` | Stores the primary user identity and credential state. | Portfolio |

### Relationships

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
  users["<b>users</b><br/>String email<br/>String username<br/>String password<br/>LocalDateTime created_at<br/>LocalDateTime deleted_at"]
  class users entity;
  portfolios -->|portfolio_id| assets
  users -->|user_id| portfolios
  users -->|user_id| refresh_tokens
```

### Schema Coverage

- The ER diagram is derived from JPA entities, so it reflects object relationships that are implemented in code now.
- Liquibase changelogs should be read together with the entities when reviewing schema evolution or rollout risk.
- Refresh-token persistence is part of the core auth contract, not just an implementation detail, because rotation and revocation depend on this table.
- Portfolio and asset tables are already present in persistence, even if the current HTTP surface is still centered on authentication.

### Constraints and Persistence Notes

- Treat JPA entities and schema-management files as the source of truth for persistence details.
- Review nullability, token revocation flags, timestamps, and foreign-key ownership in code before changing this page.
- Liquibase changelogs are present and should be checked together with entities when schema behavior changes.


## Constraints and Persistence Notes

- Review nullability, unique constraints, token lifecycle flags, timestamps, and join columns in code before changing this page.
- Prefer entities and changelogs over older Markdown when the two diverge.

## Source of Truth

- JPA entities and changelog files are the source of truth for this page.

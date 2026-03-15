# Database Overview

## Core Services

The repository implements a centralized database model for persistent data storage and retrieval.

### User Service Schema
This service manages user authentication, portfolio management, and asset tracking using a relational database structure.

## Key Entities

| Table | Entity | Type | Key Links |
| --- | --- | ---- | --------- |
| `assets` | `AssetEntity` | Asset data storage | Portfolio |
| `portfolios` | `PortfolioEntity` | User portfolio management | Asset, User |
| `refresh_tokens` | `RefreshTokenEntity` | Authentication token storage | User |
| `users` | `UserEntity` | User account management | Portfolio |

<u>`portfolio_id`</u> connects `portfolios` to `users` and `assets`.
`user_id` establishes relationships between `users`, `portfolios`, and `refresh_tokens`.

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
  "themeCSS": ".messageText, .messageText tspan, .label text, .label tspan, .edgeLabel text, .edgeLabel tspan, .nodeLabel, .nodeLabel p, .nodeLabel span, .label foreignObject, .label foreignObject div, .cluster-label text, .cluster-label tspan, .actor text, .actor tspan, .loopText, .noteText, .er text, .er tspan, .er.entityLabel, .er.attributeText, .er.relationshipLabel, .er foreignObject div, .er foreignObject span, .er foreignObject td, .er foreignObject th { fill: #ffcc66 !important; color: "#ffcc66" !important; } .messageLine0, .messageLine1, .flowchart-link, .edgePath path, .actor-line, .er.relationshipLine { stroke: "#ffb347" !important; fill: none !important; } .arrowheadPath, marker path { stroke: "#ffb347" !important; fill: "#ffb347" !important; } .er rect, .er .entityBox, .er .attributeBoxEven, .er .attributeBoxOdd { fill: "#24443d" !important; stroke: "#73e5c6" !important; } .er .relationshipLabelBox { fill: "#17312d" !important; stroke: "#57c8a8" !important; } .er foreignObject, .er foreignObject div, .er foreignObject table { background: "#24443d" !important; } .er foreignObject tr:nth-child(odd), .er foreignObject tr:nth-child(odd) td { background: "#24443d" !important; } .er foreignObject tr:nth-child(even), .er foreignObject tr:nth-child(even) td { background: "#2b4f47" !important; } .er foreignObject td, .er foreignObject th { border-color: "#73e5c6" !important; }"
}}%%
flowchart LR
  classDef entity fill:#1f3d37,stroke:#73e5c6,stroke-width:2px,color:#ffcc66;
  classDef relation fill:#17312d,stroke:#ffb347,color:#ffcc66;
  assets["**assets**<br/>**String** symbol<br/>**AssetTypeEnum** asset_type<br/>**BigDecimal** quantity<br/>**BigDecimal** average_price<br/>**CurrencyCodeEnum** currency<br/>..."]
  class assets entity;
  portfolios["**portfolios**<br/>**String** name<br/>**String** description<br/>**CurrencyCodeEnum** base_currency<br/>RiskProfileEnum risk_profile<br/>InvestmentHorizonEnum investment_horizon<br/>..."]
  class portfolios entity;
  refresh_tokens["**refresh_tokens**<br/>**String** token<br/>**Instant** expires_at<br/>**boolean** revoked<br/>**Instant** created_at"]
  class refresh_tokens entity;
  users["**users**<br/>**String** email<br/>**String** username<br/>**String** password<br/>**LocalDateTime** created_at<br/>**LocalDateTime** deleted_at"]
  class users entity;
  portfolios -->|**portfolio_id**| assets
  users -->|**user_id**| portfolios
  users -->|**user_id**| refresh_tokens
```

## Technical Considerations
- All database interactions must maintain JPA mapping integrity
- Liquibase change sets must be properly utilized for schema modifications
- Strict adherence to entity relationships and foreign key constraints required when implementing persistence layer changes

## Source of Truth
This document provides the authoritative source for the current database schema structure and relationships.

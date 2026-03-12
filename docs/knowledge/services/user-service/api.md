# User Service API

## Overview

This page consolidates the implemented HTTP surface for `user-service`. It replaces the older split between authentication, flow, and sequence pages and focuses only on contracts that are visible in code today.

## Base Paths and API Surface

| Aspect | Value |
| --- | --- |
| Base path families | `/tyche-wealth/user-service/v1/auth` |
| Source of truth | `*Api.java` contracts plus DTOs, service helpers, and centralized error handling |
| Detected APIs | `AuthApi.java` |

## Endpoint Summary

### `AuthApi.java`

| Method | Path | Purpose | Operational Note |
| --- | --- | --- | --- |
| `POST` | `/tyche-wealth/user-service/v1/auth/register` | Creates a new user account and returns the created user representation. | Creates persistent user state and is subject to dedicated registration rate limiting. |
| `POST` | `/tyche-wealth/user-service/v1/auth/login` | Authenticates a user and returns an access token together with refresh-token state. | Issues access and refresh credentials and records authentication metrics. |
| `POST` | `/tyche-wealth/user-service/v1/auth/refresh` | Validates a refresh token, rotates token state, and returns refreshed credentials. | Validates token state, rotates refresh-token persistence, and returns refreshed credentials. |

## Implemented Endpoints

### `POST /tyche-wealth/user-service/v1/auth/register`

#### Purpose

Creates a new user account and returns the created user representation.

#### Contract

| Contract Item | Value |
| --- | --- |
| Success status | `201 Created` |
| Source API | `AuthApi.java` |
| Request DTO | `RegisterRequestDto` |
| Response DTO | `UserResponseDto` |

#### Validation Snapshot

| Input | Rules |
| --- | --- |
| `email` | Email cannot be blank; Email format is invalid; Email must be at most 254 characters; Value is normalized before downstream validation and persistence checks. |
| `username` | Username cannot be blank; Username must be between 3 and 30 characters; Value is normalized before downstream validation and persistence checks. |
| `password` | Password cannot be blank; Password must be at least 8 characters; Password must include uppercase, lowercase, number and symbol |
| Cross-field checks | Password must be at most 72 bytes when UTF-8 encoded |

#### Runtime Constraints

- Service-layer checks: email and username are normalized and must remain unique before user creation proceeds.
- Dedicated rate limiting: registration requests are intercepted through the register rate-limit configuration.
- Validation failures are aggregated by the centralized `ErrorHandler` instead of being returned ad hoc from each controller method.

#### Error Behavior

| Status | When it happens |
| --- | --- |
| `400 Bad Request` | DTO validation fails, request JSON is malformed, or an auth-specific password format rule rejects the payload. |
| `409 Conflict` | Registration collides with an existing email or username, either during pre-checks or at the persistence layer. |
| `429 Too Many Requests` | The endpoint-specific rate-limit interceptor blocks the request because the active window has been exceeded. |
| Error payload shape | Centralized through `ErrorHandler`, which maps validation, auth, rate-limit, and generic failures to the API response contract. |

---

### `POST /tyche-wealth/user-service/v1/auth/login`

#### Purpose

Authenticates a user and returns an access token together with refresh-token state.

#### Contract

| Contract Item | Value |
| --- | --- |
| Success status | `200 OK` |
| Source API | `AuthApi.java` |
| Request DTO | `LoginRequestDto` |
| Response DTO | `LoginResponseDto` |

#### Validation Snapshot

| Input | Rules |
| --- | --- |
| `email` | Email cannot be blank; Email format is invalid; Email must be at most 254 characters; Value is normalized before downstream validation and persistence checks. |
| `password` | Password cannot be blank; Password must be at least 8 characters; Password must include uppercase, lowercase, number and symbol |
| Cross-field checks | Password must be at most 72 bytes when UTF-8 encoded |

#### Runtime Constraints

- Service-layer checks: email is normalized before lookup and the password must satisfy the login password policy before credential matching.
- Dedicated rate limiting: login requests are intercepted through the login rate-limit configuration.
- Validation failures are aggregated by the centralized `ErrorHandler` instead of being returned ad hoc from each controller method.

#### Error Behavior

| Status | When it happens |
| --- | --- |
| `400 Bad Request` | DTO validation fails, request JSON is malformed, or an auth-specific password format rule rejects the payload. |
| `401 Unauthorized` | Email does not resolve to a user or the provided password does not match the stored hash. |
| `429 Too Many Requests` | The endpoint-specific rate-limit interceptor blocks the request because the active window has been exceeded. |
| Error payload shape | Centralized through `ErrorHandler`, which maps validation, auth, rate-limit, and generic failures to the API response contract. |

---

### `POST /tyche-wealth/user-service/v1/auth/refresh`

#### Purpose

Validates a refresh token, rotates token state, and returns refreshed credentials.

#### Contract

| Contract Item | Value |
| --- | --- |
| Success status | `200 OK` |
| Source API | `AuthApi.java` |
| Request DTO | `RefreshTokenRequestDto` |
| Response DTO | `RefreshTokenResponseDto` |

#### Validation Snapshot

| Input | Rules |
| --- | --- |
| `refreshToken` | Refresh token cannot be blank |

#### Runtime Constraints

- Service-layer checks: the refresh token must be present, resolvable, not revoked, and still within its validity window before rotation succeeds.
- Dedicated rate limiting: refresh requests are intercepted through the refresh rate-limit configuration.
- Validation failures are aggregated by the centralized `ErrorHandler` instead of being returned ad hoc from each controller method.

#### Error Behavior

| Status | When it happens |
| --- | --- |
| `400 Bad Request` | DTO validation fails, request JSON is malformed, or an auth-specific password format rule rejects the payload. |
| `401 Unauthorized` | Refresh token is missing, invalid, revoked, expired, or otherwise rejected during refresh-token validation. |
| `429 Too Many Requests` | The endpoint-specific rate-limit interceptor blocks the request because the active window has been exceeded. |
| Error payload shape | Centralized through `ErrorHandler`, which maps validation, auth, rate-limit, and generic failures to the API response contract. |

## Flows and Sequence Diagrams

### Register Flow

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
  A[Client<br/>submits register request] --> B[API<br/>receives request]
  B --> C[Validate DTO<br/>and business rules]
  C --> D[Create user<br/>and initial state]
  D --> E[Persist user<br/>record]
  E --> F[Return created<br/>user response]
```

### Login Sequence

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
sequenceDiagram
  participant Client
  participant Api as API
  participant Auth as AuthSvc
  participant Token as Token
  participant Repo as Repo
  participant DB as DB
  Client->>Api: POST /auth/login
  Api->>Auth: login(request)
  Auth->>Auth: validate payload
  Auth->>Auth: authenticate user
  Auth->>Auth: load user context
  Auth->>Token: issue access + refresh
  Auth->>Repo: save token
  Repo->>DB: insert token row
  DB-->>Repo: row stored
  Repo-->>Auth: token persisted
  Token-->>Auth: token pair ready
  Auth-->>Api: login response
  Api-->>Client: 200 OK
```

### Refresh Sequence

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
sequenceDiagram
  participant Client
  participant Api as API
  participant Auth as AuthSvc
  participant Token as Token
  participant Repo as Repo
  participant DB as DB
  Client->>Api: POST /auth/refresh
  Api->>Auth: refresh(request)
  Auth->>Auth: validate payload
  Auth->>Repo: find token
  Repo->>DB: select token row
  DB-->>Repo: token row
  Repo-->>Auth: current token
  Auth->>Auth: validate token state
  Auth->>Auth: resolve token owner
  Auth->>Token: issue new access
  Auth->>Repo: save replacement
  Repo->>DB: update token state
  DB-->>Repo: row updated
  Token-->>Auth: new access ready
  Auth-->>Api: refresh response
  Api-->>Client: 200 OK
```

### Refresh Token Lifecycle

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
  Issue[Login issues<br/>token] --> Store[Persist token]
  Store --> Use[Client sends<br/>token]
  Use --> Validate[Validate token]
  Validate --> Rotate[Generate<br/>replacement]
  Rotate --> Revoke[Old token<br/>revoked]
  Revoke --> Persist[Persist new<br/>token state]
```

## Notes

- Treat routes not listed here as not implemented unless a concrete controller or API contract is added.
- Use controller interfaces, DTOs, service implementations, and error handlers as the source of truth for API behavior.

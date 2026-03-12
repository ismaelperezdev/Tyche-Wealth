# User Service Runtime

## Overview

This page consolidates local setup, runtime configuration, security, and operational notes for `user-service`.

## Runtime Summary

| Topic | Current State |
| --- | --- |
| Spring application name | `user-service` |
| Default local port | `8080` |
| Build tool | Maven project in the service root |
| Datasource | PostgreSQL-backed datasource configured through Spring properties |
| Security | Dedicated Spring security configuration present |
| Rate limiting | Endpoint-specific interceptor configuration present |

## Requirements

- Spring application name: `user-service`
- Default configured port: `8080`
- Requires a PostgreSQL-backed datasource according to the current service configuration.
- Build and local execution are driven from the service Maven project.

## Run Locally

- Start the backing infrastructure expected by the service, especially the configured PostgreSQL instance.
- Provide local-only overrides through `application-local.properties` in the repository root or service folder.
- Run the service with the checked-in build tooling:

```powershell
cd user-service
.\mvnw.cmd spring-boot:run
```

## Local Configuration

### Datasource

| Parameter | Description |
| --- | --- |
| `spring.datasource.driver-class-name` | JDBC driver class used by the service datasource. |
| `spring.datasource.password` | Database password for the configured datasource. |
| `spring.datasource.url` | Datasource connection URL. |
| `spring.datasource.username` | Database username for the configured datasource. |

### JWT and tokens

| Parameter | Description |
| --- | --- |
| `app.auth.jwt.access-token-ttl-seconds` | Lifetime of generated access tokens in seconds. |
| `app.auth.jwt.refresh-token-ttl-seconds` | Lifetime of generated refresh tokens in seconds. |
| `app.auth.jwt.secret` | Signing secret used for JWT generation and validation. |

### Rate limiting

| Parameter | Description |
| --- | --- |
| `app.auth.login-rate-limit.max-requests` | Maximum login requests allowed inside the configured rate-limit window. |
| `app.auth.login-rate-limit.window-seconds` | Window length in seconds for login rate limiting. |
| `app.auth.refresh-rate-limit.max-requests` | Maximum refresh requests allowed inside the configured rate-limit window. |
| `app.auth.refresh-rate-limit.window-seconds` | Window length in seconds for refresh-token rate limiting. |
| `app.auth.register-rate-limit.max-requests` | Maximum register requests allowed inside the configured rate-limit window. |
| `app.auth.register-rate-limit.window-seconds` | Window length in seconds for registration rate limiting. |

### Spring and bootstrap

| Parameter | Description |
| --- | --- |
| `server.port` | Default HTTP port exposed by the service. |
| `spring.application.name` | Logical Spring application name used by the service. |
| `spring.config.import` | Additional configuration import path resolved during startup. |
| `spring.jpa.hibernate.ddl-auto` | Hibernate schema-management mode used at runtime. |
| `spring.jpa.show-sql` | Enables SQL statement logging when set for development use. |
| `spring.liquibase.change-log` | Liquibase changelog entry point used for schema evolution. |

## Security, Rate Limiting, and Observability

- Security is configured explicitly through a dedicated Spring configuration class.
- Register, login, and refresh paths are protected by dedicated rate-limit interceptor wiring.

### Rate Limiting Flow
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
flowchart TD
  A[Incoming auth request] --> B{Path matches register/login/refresh}
  B -- No --> C[Continue normal request handling]
  B -- Yes --> D[Matching rate limit interceptor runs]
  D --> E{Limit exceeded in active window}
  E -- No --> F[Request proceeds to controller]
  E -- Yes --> G[Request blocked and metric recorded]
```

- Metrics classes record request, success, failure, and rate-limited outcomes for auth operations.

## Operational Notes

- Keep secrets out of committed configuration and out of generated documentation.
- Revisit this page whenever configuration keys, interceptors, metrics, or local startup assumptions change.
- Runtime notes are currently centered on the implemented authentication surface because that is the code-backed API exposed today.

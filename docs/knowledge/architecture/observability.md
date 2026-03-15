# Observability Architecture

## Overview

This page documents the observability flow that is currently implemented in the repository and the role of Prometheus and Grafana in the local stack.

## Repository Snapshot

| Aspect | Current State |
| --- | --- |
| Metrics producer | `user-service` |
| Metrics endpoint | `/actuator/prometheus` |
| Metrics collector | Prometheus |
| Dashboard layer | Grafana |
| Grafana repository configuration | `observability/grafana/` |

## Implemented Flow

- `user-service` exposes Prometheus-formatted metrics through Spring Boot Actuator.
- Prometheus scrapes the exposed metrics endpoint and stores the resulting time-series locally.
- Grafana uses Prometheus as its datasource and renders dashboard panels for operational inspection.

## Interaction Diagram

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
  App["user-service"] --> Endpoint["/actuator/prometheus"]
  Endpoint --> Prom["Prometheus"]
  Prom --> Graf["Grafana"]
```

## Configuration Layout

| Path | Purpose |
| --- | --- |
| `observability/grafana/provisioning/datasources/prometheus.yml` | Grafana datasource provisioning for Prometheus. |
| `observability/grafana/provisioning/dashboards/dashboards.yml` | Grafana dashboard provisioning configuration. |
| `observability/grafana/dashboards/tyche-user-service-overview.json` | Initial dashboard definition for `user-service`. |

## Metrics Families

| Metrics family | What it covers |
| --- | --- |
| `tyche_auth_*` | Auth-domain requests, outcomes, token lifecycle, and rate-limiting signals. |
| `tyche_user_*` | User-domain requests, success outcomes, and domain-specific error signals. |
| `http_server_requests_*` | Endpoint traffic, latency, and response status observations. |
| `jvm_*` | JVM memory, threads, and runtime state. |
| `jdbc_*` | Datasource and connection-pool state. |

## Notes

- The current repository contains one implemented service, so the observability flow is presently centered on `user-service`.
- Dashboard panels are operational views over Prometheus data and should be interpreted together with the selected time range and generated traffic.

# User Service Observability

## Overview

This page explains the operational checks currently provided by the `user-service` Grafana dashboard and how those checks map to the metrics exposed by the service.

## Dashboard Summary

| Topic | Current State |
| --- | --- |
| Service | `user-service` |
| Dashboard file | `observability/grafana/dashboards/tyche-user-service-overview.json` |
| Datasource | Prometheus |
| Main scope | Auth flow, user flow, HTTP behavior, and runtime health |

## Dashboard Representation

The current dashboard is organized by diagnostic intent rather than by raw metric family.
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
  A["Top row: total app requests, successful operations, error signals, rate-limited requests"]
  B["Second row: HTTP request rate by endpoint and HTTP max latency by endpoint"]
  C["Third row: auth activity in selected range and user metrics in selected range"]
  D["Bottom row: CPU usage, heap used total, live threads, HTTP 401 by endpoint"]
  A --> B --> C --> D
```

## What The Dashboard Checks

| Area | Metrics used | What it helps validate |
| --- | --- | --- |
| App totals | `tyche_auth_*`, `tyche_user_*` | Whether the service is receiving requests and producing successful or failed domain operations. |
| HTTP traffic | `http_server_requests_*` | Which endpoints are active and whether request volume or latency shifts by route. |
| Auth flow | `tyche_auth_*` | Login, refresh, token issue, token revoke, and auth rate-limiting behavior in the selected range. |
| User flow | `tyche_user_*` | Retrieve, update, password update, delete, and user-domain error activity in the selected range. |
| Runtime health | `jvm_*`, `jdbc_*`, `system_cpu_usage` | CPU, heap pressure, live threads, and datasource health. |
| Unauthorized responses | `http_server_requests_seconds_count{status="401"}` | Which endpoints are returning `401` responses in the selected range. |

## Metric Notes

- `tyche_auth_*` metrics are business-facing auth counters and should be used to inspect auth flow outcomes rather than raw HTTP behavior alone.
- `tyche_user_*` metrics are business-facing user counters and include domain signals such as unauthorized user access, not found, or password-related validation failures.
- `http_server_requests_*` metrics provide the technical HTTP view and are useful when raw response behavior needs to be correlated with domain counters.
- `jvm_*` and `jdbc_*` metrics provide runtime context and should be read as supporting health signals rather than domain outcomes.

## Operational Notes

- Some panels are range-based, so they can legitimately appear empty when the selected time window does not include traffic for that flow.
- `tyche_user_unauthorized_total` is narrower than all HTTP `401` responses, which is why the dashboard also includes a dedicated `HTTP 401 by endpoint` view.
- This page should be updated whenever the dashboard layout, Prometheus queries, or service metrics change in a way that alters what the operational view is intended to confirm.

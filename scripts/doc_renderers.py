from __future__ import annotations

from pathlib import Path
import xml.etree.ElementTree as ET

from doc_catalog import ServiceCatalog
from doc_models import DtoContract, EndpointInfo, ServiceDefinition, ServiceFacts


class BaseServiceRenderer:
    MERMAID_INIT = """%%{init: {
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
}}%%"""

    def __init__(self, catalog: ServiceCatalog, facts: ServiceFacts) -> None:
        self.catalog = catalog
        self.facts = facts
        self.service = facts.service

    def render(self, doc_path: Path) -> str | None:
        doc_type = self.catalog.infer_doc_type(doc_path)
        if doc_path == self.service.readme_path or doc_type == "service-readme":
            return self.render_service_readme()
        if doc_type == "service-overview":
            return self.render_service_overview()
        if doc_type == "service-api":
            return self.render_service_api()
        if doc_type == "service-data-model":
            return self.render_service_data_model()
        if doc_type == "service-runtime":
            return self.render_service_runtime()
        if doc_type == "service-observability":
            return self.render_service_observability()
        return None

    def _mermaid_block(self, *lines: str) -> str:
        return "\n".join(["```mermaid", self.MERMAID_INIT, *lines, "```"])

    def render_service_readme(self) -> str:
        service_title = self.service.name.replace("-", " ").title()
        docs_prefix = f"docs/knowledge/services/{self.service.name}"
        run_command = self._build_local_run_command()
        props = self.facts.application_properties
        app_name = props.get("spring.application.name", self.service.name)
        port = props.get("server.port", "unknown")
        return f"""# Tyche-Wealth - {service_title}

## Overview

`{self.service.name}` is an implemented backend service in this repository. This README acts as the fastest operational entry point for the service: what it does, how to run it, what it exposes, what it stores, and where to go next for deeper documentation.

## Service Snapshot

| Aspect | Current State |
| --- | --- |
| Service name | `{self.service.name}` |
| Spring application name | `{app_name}` |
| Default local port | `{port}` |
| Implemented endpoints | `{len(self.facts.endpoints)}` |
| Persisted entities | `{len(self.facts.entities)}` |
| Implementation slices | {self._component_groups()} |

## Responsibilities

{self._service_responsibilities()}

## Requirements

- Review build files and runtime configuration under `{self.service.name}/` before changing service behavior.
- Use the service documentation pages for API, data-model, and runtime detail.
- Keep secrets in local-only property files, never in committed source files.

## Run Locally

Typical local start path for this service:

```powershell
{run_command}
```

## Local Configuration

{self._runtime_summary_table()}

Configuration files:

- Repository root: `application-local.properties`
- Service-local overrides: `{self.service.name}/application-local.properties`

## Main Components

{self._main_components()}

## Architecture Diagram

{self.render_system_architecture()}

## Implemented Endpoints

{self._render_endpoint_summary_table()}

## Data Model Summary

{self._entity_summary_table()}

## Database Notes

{self._database_snapshot_table()}

{self._database_diagram()}

## Soft-delete Strategy

{self._soft_delete_strategy_notes()}

{self._database_schema_coverage()}

## Security and Operational Notes

{self._security_and_operational_notes()}

## Runtime Notes

{self._runtime_requirements()}

## Test Coverage View

{self._test_summary_table()}

{self._test_notes()}

## Documentation Links

| Page | Why open it |
| --- | --- |
| `{docs_prefix}/overview.md` | Service scope, responsibilities, and architecture-facing summary. |
| `{docs_prefix}/api.md` | Implemented endpoints, validation rules, and API diagrams. |
| `{docs_prefix}/data-model.md` | Entities, relationships, and persistence details. |
| `{docs_prefix}/runtime.md` | Setup, runtime configuration, security, and operations. |
| `{docs_prefix}/observability.md` | Dashboard intent, metric groups, and operational checks. |
"""

    def render_service_overview(self) -> str:
        props = self.facts.application_properties
        app_name = props.get("spring.application.name", self.service.name)
        port = props.get("server.port", "unknown")
        endpoint_count = len(self.facts.endpoints)
        entity_count = len(self.facts.entities)
        component_groups = self._component_groups()
        return f"""# {self._service_title()} Overview

## Overview

`{self.service.name}` is an implemented Spring Boot service in this repository. The configured application name is `{app_name}` and the default local port is `{port}`. At the current repository state it is one of `{len(self.catalog.services)}` implemented backend services detected in code.

## At a Glance

| Aspect | Current State |
| --- | --- |
| Service name | `{self.service.name}` |
| Spring application name | `{app_name}` |
| Default local port | `{port}` |
| Detected HTTP endpoints | `{endpoint_count}` |
| Detected persisted entities | `{entity_count}` |
| Implementation slices | {component_groups} |

## Responsibilities

{self._service_responsibilities()}

## Implemented Scope

- Detected HTTP endpoints: `{endpoint_count}`
- Detected persisted entities: `{entity_count}`
- Detected implementation slices: {component_groups}
- The implemented surface should be read from controllers, services, helpers, repositories, entities, and configuration classes rather than from older Markdown pages.

## Main Components

{self._main_components()}

## Security and Operational Notes

{self._security_and_operational_notes()}

## Related Documentation

- `docs/knowledge/services/{self.service.name}/api.md`
- `docs/knowledge/services/{self.service.name}/data-model.md`
- `docs/knowledge/services/{self.service.name}/runtime.md`
- `docs/knowledge/services/{self.service.name}/observability.md`
- `docs/knowledge/project-context.md`
"""

    def render_service_api(self) -> str:
        endpoints = "\n\n---\n\n".join(self._render_endpoint_details(endpoint) for endpoint in self.facts.endpoints)
        if not endpoints:
            endpoints = "No implemented endpoints were detected in the API contracts for this service."
        endpoint_summary = self._render_endpoint_summary_table()
        flow_diagrams = self._api_flow_sections()
        return f"""# {self._service_title()} API

## Overview

This page consolidates the implemented HTTP surface for `{self.service.name}`. It replaces the older split between authentication, flow, and sequence pages and focuses only on contracts that are visible in code today.

## Base Paths and API Surface

{self._base_paths_summary()}

## Endpoint Summary

{endpoint_summary}

## Implemented Endpoints

{endpoints}

## Flows and Sequence Diagrams

{flow_diagrams}

## Notes

- Treat routes not listed here as not implemented unless a concrete controller or API contract is added.
- Use controller interfaces, DTOs, service implementations, and error handlers as the source of truth for API behavior.
"""

    def render_service_data_model(self) -> str:
        entity_sections = "\n\n".join(self._render_entity_section(entity) for entity in self.facts.entities)
        if not entity_sections:
            entity_sections = "No persisted entities were detected in the current service sources."
        return f"""# {self._service_title()} Data Model

## Overview

This page consolidates the persistence model for `{self.service.name}`. It replaces the older split between table pages, schema notes, and entity summaries.

## Entity Summary

{self._entity_summary_table()}

## Implemented Entities

{entity_sections}

## Relationships

{self._database_diagram()}

## Persistence and Schema Notes

{self._persistence_notes()}

## Related Documentation

- `docs/knowledge/services/{self.service.name}/overview.md`
- `docs/knowledge/services/{self.service.name}/runtime.md`
- `docs/knowledge/database/overview.md`
"""

    def render_service_runtime(self) -> str:
        return f"""# {self._service_title()} Runtime

## Overview

This page consolidates local setup, runtime configuration, security, and operational notes for `{self.service.name}`.

## Runtime Summary

{self._runtime_summary_table()}

## Requirements

{self._runtime_requirements()}

## Run Locally

{self._runtime_run_steps()}

## Local Configuration

{self._runtime_configuration_notes()}

## Security, Rate Limiting, and Observability

{self._runtime_operational_controls()}

## Operational Notes

{self._runtime_limitations()}
"""

    def render_service_observability(self) -> str:
        return f"""# {self._service_title()} Observability

## Overview

This page explains the operational checks currently provided by the `{self.service.name}` Grafana dashboard and how those checks map to the metrics exposed by the service.

## Dashboard Summary

| Topic | Current State |
| --- | --- |
| Service | `{self.service.name}` |
| Dashboard file | `observability/grafana/dashboards/tyche-user-service-overview.json` |
| Datasource | Prometheus |
| Main scope | Auth flow, user flow, HTTP behavior, and runtime health |

## Dashboard Representation

The current dashboard is organized by diagnostic intent rather than by raw metric family.

{self._service_observability_layout()}

## What The Dashboard Checks

{self._service_observability_checks()}

## Metric Notes

{self._service_observability_notes()}

## Operational Notes

- Some panels are range-based, so they can legitimately appear empty when the selected time window does not include traffic for that flow.
- `tyche_user_unauthorized_total` is narrower than all HTTP `401` responses, which is why the dashboard also includes a dedicated `HTTP 401 by endpoint` view.
- This page should be updated whenever the dashboard layout, Prometheus queries, or service metrics change in a way that alters what the operational view is intended to confirm.
"""

    def render_database_overview(self) -> str:
        tables = self._database_entity_inventory()
        return f"""## {self.service.name}

### Snapshot

{self._database_snapshot_table()}

### Implemented Entities

{tables}

### Relationships

{self._database_diagram()}

### Schema Coverage

{self._database_schema_coverage()}

### Constraints and Persistence Notes

{self._persistence_notes()}
"""

    def render_system_architecture(self) -> str:
        has_security = self._path_exists("config/SecurityConfig.java")
        has_rate_limit = self._path_exists("config/RefreshRateLimitConfig.java")
        lines = [
            "flowchart LR",
            '  Client[Client App] --> Api["REST API / Controllers"]',
            '  Api --> Service["Service Layer and Helpers"]',
            '  Service --> Repo["Repositories"]',
            '  Repo --> Db[("PostgreSQL")]',
        ]
        if has_security:
            lines.append('  Api --> Security["Security Config"]')
        if has_rate_limit:
            lines.append('  Api --> RateLimit["Rate Limit Interceptors"]')
        return self._mermaid_block(*lines)

    def render_observability_architecture(self) -> str:
        return self._mermaid_block(
            "flowchart LR",
            '  App["user-service"] --> Endpoint["/actuator/prometheus"]',
            '  Endpoint --> Prom["Prometheus"]',
            '  Prom --> Graf["Grafana"]',
        )

    def render_system_summary(self) -> str:
        props = self.facts.application_properties
        port = props.get("server.port", "unknown")
        app_name = props.get("spring.application.name", self.service.name)
        return "\n".join(
            [
                "| Aspect | Current State |",
                "| --- | --- |",
                f"| Service | `{self.service.name}` |",
                f"| Spring application name | `{app_name}` |",
                f"| Default local port | `{port}` |",
                f"| HTTP endpoints detected | `{len(self.facts.endpoints)}` |",
                f"| Persisted entities detected | `{len(self.facts.entities)}` |",
                f"| Implementation slices | {self._component_groups()} |",
            ]
        )

    def render_cross_cutting_summary(self) -> str:
        lines = [
            "| Concern | Current State |",
            "| --- | --- |",
            "| Security | Dedicated configuration detected. |" if self._path_exists("config/SecurityConfig.java") else "| Security | No dedicated configuration detected. |",
            "| Rate limiting | Interceptor-based auth rate limiting detected. |" if self._path_exists("config/RefreshRateLimitConfig.java") else "| Rate limiting | No explicit rate-limiting configuration detected. |",
            "| Observability | Auth metrics instrumentation detected. |" if self._path_exists("service/monitoring/AuthMetrics.java") else "| Observability | No explicit service metrics helper detected. |",
            "| Persistence | JPA entities and repositories are present. |" if self.facts.entities else "| Persistence | No persisted entities detected. |",
        ]
        return "\n".join(lines)

    def _service_title(self) -> str:
        return self.service.name.replace("-", " ").title()

    def _build_local_run_command(self) -> str:
        if (self.service.root / "mvnw.cmd").exists():
            return f"cd {self.service.name}\n.\\mvnw.cmd spring-boot:run"
        if (self.service.root / "mvnw").exists():
            return f"cd {self.service.name}\n./mvnw spring-boot:run"
        return f"cd {self.service.name}\nmvn spring-boot:run"

    def _component_groups(self) -> str:
        groups: set[str] = set()
        java_root = self.service.root / "src" / "main" / "java"
        if java_root.exists():
            for path in java_root.rglob("*"):
                if not path.is_file():
                    continue
                relative = path.relative_to(java_root).as_posix().lower()
                for token in ("controller", "service", "helper", "repository", "entity", "config", "web", "dto", "mapper"):
                    if token in relative:
                        groups.add(token)
        return ", ".join(f"`{item}`" for item in sorted(groups)) or "`no obvious slices detected`"

    def _service_responsibilities(self) -> str:
        if self._has_auth_surface():
            return "\n".join(
                [
                    "- Exposes the implemented authentication API for register, login, and refresh operations.",
                    "- Coordinates validation, token generation, refresh-token lifecycle handling, and persistence updates.",
                    "- Stores user-facing auth state and related portfolio or asset ownership data through JPA entities and repositories.",
                ]
            )
        return "\n".join(
            [
                "- Exposes the service API contracts currently visible in code.",
                "- Coordinates business logic through service and helper classes.",
                "- Persists service state through repositories, entities, and schema management files.",
            ]
        )

    def _main_components(self) -> str:
        parts = [
            "- API contracts and controller implementations define the externally visible HTTP surface.",
            "- Service and helper classes contain orchestration, validation, token, and domain logic.",
            "- Repositories and entities define persisted state and object relationships.",
            "- Configuration and web classes provide security, rate limiting, and request interception support.",
        ]
        return "\n".join(parts)

    def _service_observability_layout(self) -> str:
        return self._mermaid_block(
            "flowchart TD",
            '  A["Top row: total app requests, successful operations, error signals, rate-limited requests"]',
            '  B["Second row: HTTP request rate by endpoint and HTTP max latency by endpoint"]',
            '  C["Third row: auth activity in selected range and user metrics in selected range"]',
            '  D["Bottom row: CPU usage, heap used total, live threads, HTTP 401 by endpoint"]',
            "  A --> B --> C --> D",
        )

    def _service_observability_checks(self) -> str:
        return "\n".join(
            [
                "| Area | Metrics used | What it helps validate |",
                "| --- | --- | --- |",
                "| App totals | `tyche_auth_*`, `tyche_user_*` | Whether the service is receiving requests and producing successful or failed domain operations. |",
                "| HTTP traffic | `http_server_requests_*` | Which endpoints are active and whether request volume or latency shifts by route. |",
                "| Auth flow | `tyche_auth_*` | Login, refresh, token issue, token revoke, and auth rate-limiting behavior in the selected range. |",
                "| User flow | `tyche_user_*` | Retrieve, update, password update, delete, and user-domain error activity in the selected range. |",
                "| Runtime health | `jvm_*`, `jdbc_*`, `system_cpu_usage` | CPU, heap pressure, live threads, and datasource health. |",
                '| Unauthorized responses | `http_server_requests_seconds_count{status="401"}` | Which endpoints are returning `401` responses in the selected range. |',
            ]
        )

    def _service_observability_notes(self) -> str:
        return "\n".join(
            [
                "- `tyche_auth_*` metrics are business-facing auth counters and should be used to inspect auth flow outcomes rather than raw HTTP behavior alone.",
                "- `tyche_user_*` metrics are business-facing user counters and include domain signals such as unauthorized user access, not found, or password-related validation failures.",
                "- `http_server_requests_*` metrics provide the technical HTTP view and are useful when raw response behavior needs to be correlated with domain counters.",
                "- `jvm_*` and `jdbc_*` metrics provide runtime context and should be read as supporting health signals rather than domain outcomes.",
            ]
        )

    def _database_snapshot_table(self) -> str:
        return "\n".join(
            [
                "| Aspect | Current State |",
                "| --- | --- |",
                f"| Service | `{self.service.name}` |",
                f"| Detected entities | `{len(self.facts.entities)}` |",
                f"| Tables represented | `{len({entity.table_name for entity in self.facts.entities})}` |",
                f"| Many-to-one relations | `{sum(1 for entity in self.facts.entities for relation in entity.relations if relation.relation_type == 'many-to-one')}` |",
                "| Liquibase changelogs | Present |" if (self.service.root / "src" / "main" / "resources" / "db.changelog").exists() else "| Liquibase changelogs | Not detected |",
            ]
        )

    def _database_entity_inventory(self) -> str:
        if not self.facts.entities:
            return "No entities detected."
        lines = [
            "| Table | Entity | Role | Key Links |",
            "| --- | --- | --- | --- |",
        ]
        for entity in self.facts.entities:
            relation_summary = ", ".join(
                sorted(
                    {
                        relation.target_entity.replace("Entity", "")
                        for relation in entity.relations
                    }
                )
            ) or "None"
            lines.append(
                f"| `{entity.table_name}` | `{entity.class_name}` | {self._entity_purpose(entity.class_name)} | {relation_summary} |"
            )
        return "\n".join(lines)

    def _database_schema_coverage(self) -> str:
        notes = [
            "- The ER diagram is derived from JPA entities, so it reflects object relationships that are implemented in code now.",
            "- Liquibase changelogs should be read together with the entities when reviewing schema evolution or rollout risk.",
        ]
        if any(entity.table_name == "refresh_tokens" for entity in self.facts.entities):
            notes.append("- Refresh-token persistence is part of the core auth contract, not just an implementation detail, because rotation and revocation depend on this table.")
        if any(entity.table_name == "portfolios" for entity in self.facts.entities):
            notes.append("- Portfolio and asset tables are already present in persistence, even if the current HTTP surface is still centered on authentication.")
        return "\n".join(notes)

    def _soft_delete_strategy_notes(self) -> str:
        return "\n".join(
            [
                "- `DELETE /user/me` performs a soft delete: `UserServiceImpl.delete()` delegates to `userHelper.softDelete()`, which revokes active refresh tokens, sets `deletedAt`, and saves the user record instead of removing the row.",
                "- Active-user lookups are filtered through repository methods such as `findByIdAndDeletedAtIsNull()`, `findByEmailAndDeletedAtIsNull()`, and `findByUsernameAndDeletedAtIsNull()` so soft-deleted users are excluded from normal service flows.",
                "- Related data is not cascaded away by the current mappings. `PortfolioEntity.user` and `RefreshTokenEntity.user` are `@ManyToOne(optional = false)` with foreign-key constraints from `portfolios.user_id` and `refresh_tokens.user_id` to `users.id`. Soft delete preserves those rows; refresh tokens are explicitly revoked, while portfolios remain linked to the retained user row.",
                "- No restore pathway is implemented in the current code. There is no service or repository method that clears `deletedAt`, so account recovery would require a new code path or direct data repair outside the implemented API.",
            ]
        )

    def _test_summary_table(self) -> str:
        test_root = self.service.root / "src" / "test" / "java"
        if not test_root.exists():
            return "No test sources were detected."
        categories = {
            "Integration": list(test_root.rglob("*IntegrationTest.java")),
            "Repository": list(test_root.rglob("*RepositoryTest.java")),
            "Mapper": list(test_root.rglob("*MapperTest.java")),
            "Web / Interceptor": list(test_root.rglob("*InterceptorTest.java")),
            "Service Helper": list(test_root.rglob("*HelperTest.java")),
            "Application Smoke": list(test_root.rglob("*ApplicationTests.java")),
        }
        lines = [
            "| Test Area | Current State |",
            "| --- | --- |",
        ]
        for label, items in categories.items():
            if items:
                lines.append(f"| {label} | `{len(items)}` files |")
        fixture_count = len(list((self.service.root / "src" / "test" / "resources").rglob("*"))) if (self.service.root / "src" / "test" / "resources").exists() else 0
        lines.append(f"| Test resource files | `{fixture_count}` fixtures and auxiliary files |")
        current_coverage = self._read_jacoco_coverage()
        if current_coverage:
            lines.append(f"| Current line coverage | `{current_coverage['line_ratio']}` |")
            lines.append(f"| Current branch coverage | `{current_coverage['branch_ratio']}` |")
        if self._has_jacoco_plugin():
            lines.append("| Coverage instrumentation | JaCoCo Maven plugin is configured in the service build lifecycle. |")
            lines.append(f"| Coverage report | HTML report is generated at `{self.service.name}/target/site/jacoco/index.html` after `mvn verify`. |")
        return "\n".join(lines)

    def _test_notes(self) -> str:
        notes: list[str] = []
        test_root = self.service.root / "src" / "test" / "java"
        if not test_root.exists():
            return "- No automated test sources were detected for this service."
        if list(test_root.rglob("*IntegrationTest.java")):
            notes.append("- Integration coverage is present around the auth controller flow, so the HTTP contract is not documented in isolation from tests.")
        if list(test_root.rglob("*RepositoryTest.java")):
            notes.append("- Repository tests cover the persistence layer directly, which is useful when changing entities, queries, or Liquibase-backed assumptions.")
        if list(test_root.rglob("*InterceptorTest.java")):
            notes.append("- Rate-limiting and web interception behavior has dedicated tests, which matters because auth throttling is part of the live contract.")
        if list(test_root.rglob("*MapperTest.java")):
            notes.append("- Mapper tests exist, so DTO and entity translation logic is not left completely implicit.")
        if list(test_root.rglob("*HelperTest.java")):
            notes.append("- Helper-layer tests exist for auth validation, which reduces the risk of drifting request-validation behavior.")
        current_coverage = self._read_jacoco_coverage()
        if current_coverage:
            notes.append(f"- Current JaCoCo totals are `{current_coverage['line_ratio']}` line coverage and `{current_coverage['branch_ratio']}` branch coverage, based on the latest generated report in `target/site/jacoco/jacoco.xml`.")
        if self._has_jacoco_plugin():
            notes.append("- JaCoCo is wired into the Maven `verify` phase, so the service can publish a coverage report instead of relying only on raw test counts.")
            notes.append(f"- Coverage review should start from `{self.service.name}/target/site/jacoco/index.html` after a local or CI `mvn verify` run.")
            if not current_coverage:
                notes.append("- No current coverage percentage is shown because `target/site/jacoco/jacoco.xml` is not present yet for this service.")
        return "\n".join(notes) if notes else "- Test sources exist, but no categorized coverage summary was derived."

    def _security_and_operational_notes(self) -> str:
        notes = []
        if self._path_exists("config/SecurityConfig.java"):
            notes.append("- Password handling is centralized in `SecurityConfig` through a `BCryptPasswordEncoder`, so raw credentials are not persisted directly from controller input.")
        if self._path_exists("service/helper/AuthTokenHelper.java"):
            access_ttl = self.facts.application_properties.get("app.auth.jwt.access-token-ttl-seconds", "900")
            notes.append(f"- Access tokens are signed as JWTs with `HS256`; the signing secret is injected from `app.auth.jwt.secret`, and the configured access-token TTL is `{access_ttl}` seconds.")
        if self._path_exists("service/helper/AuthRefreshTokenHelper.java"):
            refresh_ttl = self.facts.application_properties.get("app.auth.jwt.refresh-token-ttl-seconds", "1209600")
            notes.append(f"- Refresh tokens are generated with `SecureRandom`, encoded for transport, persisted in the `refresh_tokens` table, and revoked or rotated on use; the configured refresh-token TTL is `{refresh_ttl}` seconds.")
        if self._path_exists("config/RefreshRateLimitConfig.java"):
            notes.append("- Register, login, and refresh routes are protected by dedicated MVC interceptors. Throttling is keyed by `HttpServletRequest.getRemoteAddr()` and enforced before the request reaches controller logic.")
            notes.extend(self._rate_limit_configuration_notes())
        if self._path_exists("service/monitoring/AuthMetrics.java"):
            notes.append("- Metrics are emitted for auth requests, successes, failures, invalid credentials, token issuance, token revocation, and rate-limited outcomes, which makes abuse patterns and auth regressions observable.")
        if "spring.config.import" in self.facts.application_properties:
            notes.append("- Secrets are expected from environment variables or local-only property imports, which keeps JWT secrets and datasource credentials out of committed defaults.")
        if not notes:
            notes.append("- No service-specific operational controls were detected beyond the standard Spring Boot setup.")
        return "\n".join(notes)

    def _has_jacoco_plugin(self) -> bool:
        pom_path = self.service.root / "pom.xml"
        if not pom_path.exists():
            return False
        pom_text = pom_path.read_text(encoding="utf-8")
        return "jacoco-maven-plugin" in pom_text

    def _read_jacoco_coverage(self) -> dict[str, str] | None:
        report_path = self.service.root / "target" / "site" / "jacoco" / "jacoco.xml"
        if not report_path.exists():
            return None
        try:
            root = ET.fromstring(report_path.read_text(encoding="utf-8"))
        except (ET.ParseError, OSError):
            return None

        counters = {counter.attrib.get("type"): counter.attrib for counter in root.findall("counter")}
        line_ratio = self._format_coverage_ratio(counters.get("LINE"))
        branch_ratio = self._format_coverage_ratio(counters.get("BRANCH"))
        if line_ratio is None and branch_ratio is None:
            return None
        return {
            "line_ratio": line_ratio or "N/A",
            "branch_ratio": branch_ratio or "N/A",
        }

    def _format_coverage_ratio(self, counter: dict[str, str] | None) -> str | None:
        if not counter:
            return None
        missed = int(counter.get("missed", "0"))
        covered = int(counter.get("covered", "0"))
        total = missed + covered
        if total == 0:
            return "0.00%"
        return f"{(covered / total) * 100:.2f}%"

    def _rate_limit_configuration_notes(self) -> list[str]:
        props = self.facts.application_properties
        notes: list[str] = []
        register_max = props.get("app.auth.register-rate-limit.max-requests")
        register_window = props.get("app.auth.register-rate-limit.window-seconds")
        if register_max and register_window:
            notes.append(f"- Registration is limited to `{register_max}` requests per `{register_window}` seconds per client address.")
        login_max = props.get("app.auth.login-rate-limit.max-requests")
        login_window = props.get("app.auth.login-rate-limit.window-seconds")
        if login_max and login_window:
            notes.append(f"- Login is limited to `{login_max}` requests per `{login_window}` seconds per client address.")
        refresh_max = props.get("app.auth.refresh-rate-limit.max-requests")
        refresh_window = props.get("app.auth.refresh-rate-limit.window-seconds")
        if refresh_max and refresh_window:
            notes.append(f"- Refresh is limited to `{refresh_max}` requests per `{refresh_window}` seconds per client address.")
        return notes

    def _base_paths_summary(self) -> str:
        unique_prefixes = sorted({self._path_prefix(endpoint.path) for endpoint in self.facts.endpoints if endpoint.path})
        if not unique_prefixes:
            return "- No stable base path could be derived from the current API contracts."
        lines = [
            "| Aspect | Value |",
            "| --- | --- |",
            f"| Base path families | {', '.join(f'`{prefix}`' for prefix in unique_prefixes)} |",
            "| Source of truth | `*Api.java` contracts plus DTOs, service helpers, and centralized error handling |",
            f"| Detected APIs | {', '.join(sorted({f'`{endpoint.source_api}`' for endpoint in self.facts.endpoints})) or 'None'} |",
        ]
        return "\n".join(lines)

    def _render_endpoint_details(self, endpoint: EndpointInfo) -> str:
        purpose = self._endpoint_purpose(endpoint)
        contract_table = self._render_endpoint_contract_table(endpoint)
        validation_table = self._render_endpoint_validation_table(endpoint)
        runtime_constraints = self._render_endpoint_runtime_constraints(endpoint)
        error_table = self._render_endpoint_error_table(endpoint)
        return (
            f"### `{endpoint.http_method} {endpoint.path}`\n"
            "\n"
            f"#### Purpose\n\n{purpose}\n\n"
            "#### Contract\n\n"
            f"{contract_table}\n\n"
            "#### Validation Snapshot\n\n"
            f"{validation_table}\n\n"
            "#### Runtime Constraints\n\n"
            f"{runtime_constraints}\n\n"
            "#### Error Behavior\n\n"
            f"{error_table}"
        )

    def _render_endpoint_summary_table(self) -> str:
        if not self.facts.endpoints:
            return "No implemented endpoint summary is available."
        grouped: dict[str, list[EndpointInfo]] = {}
        for endpoint in self.facts.endpoints:
            grouped.setdefault(endpoint.source_api, []).append(endpoint)

        sections: list[str] = []
        for source_api, endpoints in sorted(grouped.items()):
            sections.append(f"### `{source_api}`")
            sections.append("")
            sections.append("| Method | Path | Purpose | Operational Note |")
            sections.append("| --- | --- | --- | --- |")
            for endpoint in endpoints:
                sections.append(
                    f"| `{endpoint.http_method}` | `{endpoint.path}` | {self._endpoint_purpose(endpoint)} | {self._endpoint_operational_note(endpoint)} |"
                )
            sections.append("")
        return "\n".join(sections).strip()

    def _endpoint_operational_note(self, endpoint: EndpointInfo) -> str:
        path = endpoint.path.lower()
        if "register" in path:
            return "Persists a new active user record and is subject to dedicated registration rate limiting and uniqueness checks."
        if "login" in path:
            return "Validates credentials, revokes any previously active refresh tokens for the user, issues a new access token and refresh token, and records auth metrics."
        if "refresh" in path:
            return "Revokes the submitted active refresh token, persists a replacement refresh token, returns a new access token, and fails with `401` when the provided refresh token is invalid, expired, or already revoked."
        if "logout" in path:
            return "Requires a valid refresh-token request body, revokes the submitted active refresh token, and returns `204 No Content`; it does not implement server-side access-JWT invalidation or cache cleanup."
        if endpoint.http_method.upper() == "GET" and path.endswith("/user/me"):
            return "Requires a valid `Authorization: Bearer <token>` header for an active non-deleted user and returns only the mapped user DTO fields."
        if endpoint.http_method.upper() == "PATCH" and path.endswith("/user/me"):
            return "Requires a valid bearer token for an active non-deleted user, enforces username availability checks, persists the update, and returns the updated user DTO."
        if endpoint.http_method.upper() == "PATCH" and path.endswith("/user/me/password"):
            return "Requires a valid bearer token for an active non-deleted user, validates the current password, updates the stored password hash, revokes active refresh tokens, and returns `204 No Content`."
        if endpoint.http_method.upper() == "DELETE" and path.endswith("/user/me"):
            return "Requires a valid bearer token for an active non-deleted user, revokes active refresh tokens, performs a soft delete by setting `deletedAt`, and returns `204 No Content`."
        return "Backed by code-visible controller and service flow."

    def _api_flow_sections(self) -> str:
        sections: list[str] = []
        flow_builders = [
            ("Register Flow", self.render_register_flow()),
            ("Login Sequence", self.render_login_sequence()),
            ("Refresh Sequence", self.render_refresh_sequence()),
            ("Refresh Token Lifecycle", self.render_refresh_token_lifecycle()),
        ]
        for title, block in flow_builders:
            if block:
                sections.append(f"### {title}\n\n{block}")
        return "\n\n".join(sections) or "No code-backed flow or sequence diagrams were generated for this service."

    def _render_entity_section(self, entity) -> str:
        purpose = self._entity_purpose(entity.class_name)
        field_lines = self._entity_fields_table(entity)
        relation_lines = "\n".join(self._describe_relation(relation) for relation in entity.relations) or "- No relations were parsed."
        return (
            f"### `{entity.class_name}` (`{entity.table_name}`)\n\n"
            f"- Role: {purpose}\n"
            "\n#### Fields\n\n"
            f"{field_lines}\n\n"
            "#### Relations\n\n"
            f"{relation_lines}"
        )

    def _database_diagram(self) -> str:
        if not self.facts.entities:
            return "No ER diagram could be generated because no persisted entities were detected."

        lines = [
            "flowchart LR",
            "  classDef entity fill:#1f3d37,stroke:#73e5c6,stroke-width:2px,color:#ffcc66;",
            "  classDef relation fill:#17312d,stroke:#ffb347,color:#ffcc66;",
        ]
        for entity in self.facts.entities:
            label_lines = [f"<b>{entity.table_name}</b>"]
            for field in entity.fields[:5]:
                label_lines.append(f"{field.type_name} {field.name}")
            if len(entity.fields) > 5:
                label_lines.append("...")
            node_label = "<br/>".join(label_lines)
            lines.append(f'  {entity.table_name}["{node_label}"]')
            lines.append(f"  class {entity.table_name} entity;")

        for entity in self.facts.entities:
            for relation in entity.relations:
                if relation.relation_type != "many-to-one":
                    continue
                target_table = next(
                    (item.table_name for item in self.facts.entities if item.class_name == relation.target_entity),
                    relation.target_entity,
                )
                relation_label = relation.join_column or "relation"
                lines.append(f'  {target_table} -->|{relation_label}| {entity.table_name}')

        return self._mermaid_block(*lines)

    def _persistence_notes(self) -> str:
        notes = [
            "- Treat JPA entities and schema-management files as the source of truth for persistence details.",
            "- Review nullability, token revocation flags, timestamps, and foreign-key ownership in code before changing this page.",
        ]
        if (self.service.root / "src" / "main" / "resources" / "db.changelog").exists():
            notes.append("- Liquibase changelogs are present and should be checked together with entities when schema behavior changes.")
        return "\n".join(notes)

    def _entity_summary_table(self) -> str:
        if not self.facts.entities:
            return "No entity summary is available."
        lines = [
            "| Entity | Table | Role | Key Relations |",
            "| --- | --- | --- | --- |",
        ]
        for entity in self.facts.entities:
            relation_summary = ", ".join(sorted({relation.target_entity for relation in entity.relations})) or "None"
            lines.append(
                f"| `{entity.class_name}` | `{entity.table_name}` | {self._entity_purpose(entity.class_name)} | {relation_summary} |"
            )
        return "\n".join(lines)

    def _runtime_requirements(self) -> str:
        props = self.facts.application_properties
        lines = [
            f"- Spring application name: `{props.get('spring.application.name', self.service.name)}`",
            f"- Default configured port: `{props.get('server.port', 'unknown')}`",
            "- Requires a PostgreSQL-backed datasource according to the current service configuration.",
        ]
        if (self.service.root / "pom.xml").exists():
            lines.append("- Build and local execution are driven from the service Maven project.")
        return "\n".join(lines)

    def _runtime_summary_table(self) -> str:
        props = self.facts.application_properties
        lines = [
            "| Topic | Current State |",
            "| --- | --- |",
            f"| Spring application name | `{props.get('spring.application.name', self.service.name)}` |",
            f"| Default local port | `{props.get('server.port', 'unknown')}` |",
            "| Build tool | Maven project in the service root |" if (self.service.root / "pom.xml").exists() else "| Build tool | Not detected |",
            "| Datasource | PostgreSQL-backed datasource configured through Spring properties |",
            "| Security | Dedicated Spring security configuration present |" if self._path_exists("config/SecurityConfig.java") else "| Security | No dedicated security configuration detected |",
            "| Rate limiting | Endpoint-specific interceptor configuration present |" if self._path_exists("config/RefreshRateLimitConfig.java") else "| Rate limiting | No rate-limiting configuration detected |",
        ]
        return "\n".join(lines)

    def _runtime_run_steps(self) -> str:
        steps = [
            "- Start the backing infrastructure expected by the service, especially the configured PostgreSQL instance.",
            "- Provide local-only overrides through `application-local.properties` in the repository root or service folder.",
            "- Run the service with the checked-in build tooling:",
            "",
            "```powershell",
            self._build_local_run_command(),
            "```",
        ]
        return "\n".join(steps)

    def _runtime_configuration_notes(self) -> str:
        grouped: dict[str, list[tuple[str, str]]] = {
            "Datasource": [],
            "JWT and tokens": [],
            "Rate limiting": [],
            "Spring and bootstrap": [],
        }
        for key in sorted(self.facts.application_properties):
            lowered = key.lower()
            if "datasource" in lowered:
                grouped["Datasource"].append((key, self._describe_runtime_key(key)))
            elif "jwt" in lowered or "token" in lowered:
                grouped["JWT and tokens"].append((key, self._describe_runtime_key(key)))
            elif "rate" in lowered:
                grouped["Rate limiting"].append((key, self._describe_runtime_key(key)))
            elif lowered.startswith("spring.") or lowered.startswith("server."):
                grouped["Spring and bootstrap"].append((key, self._describe_runtime_key(key)))
        sections = []
        for title, rows in grouped.items():
            if not rows:
                continue
            sections.append(f"### {title}\n")
            sections.append("| Parameter | Description |")
            sections.append("| --- | --- |")
            sections.extend(f"| `{key}` | {description} |" for key, description in rows)
            sections.append("")
        return "\n".join(sections).strip() or "- No notable runtime keys were parsed from the service configuration."

    def _runtime_operational_controls(self) -> str:
        intro_lines = []
        diagram_section: list[str] = []
        outro_lines = []
        if self._path_exists("config/SecurityConfig.java"):
            intro_lines.append("- Security is configured explicitly through a dedicated Spring configuration class.")
        if self._path_exists("config/RefreshRateLimitConfig.java"):
            intro_lines.append("- Register, login, and refresh paths are protected by dedicated rate-limit interceptor wiring.")
            diagram_section.extend(["### Rate Limiting Flow", self.render_rate_limit_flow()])
        if self._path_exists("service/monitoring/AuthMetrics.java"):
            outro_lines.append("- Metrics classes record request, success, failure, and rate-limited outcomes for auth operations.")
        if not intro_lines and not outro_lines:
            return "- No explicit service-specific operational controls were detected in the current sources."
        sections: list[str] = []
        if intro_lines:
            sections.append("\n".join(intro_lines))
        if diagram_section:
            sections.append("\n".join(diagram_section))
        if outro_lines:
            sections.append("\n".join(outro_lines))
        return "\n\n".join(sections)

    def _runtime_limitations(self) -> str:
        lines = [
            "- Keep secrets out of committed configuration and out of generated documentation.",
            "- Revisit this page whenever configuration keys, interceptors, metrics, or local startup assumptions change.",
        ]
        if self._has_auth_surface():
            lines.append("- Runtime notes are currently centered on the implemented authentication surface because that is the code-backed API exposed today.")
        return "\n".join(lines)

    def _describe_runtime_key(self, key: str) -> str:
        lowered = key.lower()
        descriptions = {
            "spring.datasource.driver-class-name": "JDBC driver class used by the service datasource.",
            "spring.datasource.password": "Database password for the configured datasource.",
            "spring.datasource.url": "Datasource connection URL.",
            "spring.datasource.username": "Database username for the configured datasource.",
            "app.auth.jwt.access-token-ttl-seconds": "Lifetime of generated access tokens in seconds.",
            "app.auth.jwt.refresh-token-ttl-seconds": "Lifetime of generated refresh tokens in seconds.",
            "app.auth.jwt.secret": "Signing secret used for JWT generation and validation.",
            "app.auth.login-rate-limit.max-requests": "Maximum login requests allowed inside the configured rate-limit window.",
            "app.auth.login-rate-limit.window-seconds": "Window length in seconds for login rate limiting.",
            "app.auth.refresh-rate-limit.max-requests": "Maximum refresh requests allowed inside the configured rate-limit window.",
            "app.auth.refresh-rate-limit.window-seconds": "Window length in seconds for refresh-token rate limiting.",
            "app.auth.register-rate-limit.max-requests": "Maximum register requests allowed inside the configured rate-limit window.",
            "app.auth.register-rate-limit.window-seconds": "Window length in seconds for registration rate limiting.",
            "server.port": "Default HTTP port exposed by the service.",
            "spring.application.name": "Logical Spring application name used by the service.",
            "spring.config.import": "Additional configuration import path resolved during startup.",
            "spring.jpa.hibernate.ddl-auto": "Hibernate schema-management mode used at runtime.",
            "spring.jpa.show-sql": "Enables SQL statement logging when set for development use.",
            "spring.liquibase.change-log": "Liquibase changelog entry point used for schema evolution.",
        }
        if key in descriptions:
            return descriptions[key]
        if "datasource" in lowered:
            return "Datasource-related runtime setting."
        if "jwt" in lowered or "token" in lowered:
            return "Authentication or token-management setting."
        if "rate" in lowered:
            return "Rate-limiting configuration value."
        if lowered.startswith("spring.") or lowered.startswith("server."):
            return "Spring Boot runtime configuration key."
        return "Runtime configuration key used by the service."

    def render_register_flow(self) -> str | None:
        endpoint = self._find_endpoint("register")
        if not endpoint:
            return None
        return self._mermaid_block(
                "flowchart LR",
                "  A[Client<br/>submits register request] --> B[API<br/>receives request]",
                "  B --> C[Validate DTO<br/>and business rules]",
                "  C --> D[Create user<br/>and initial state]",
                "  D --> E[Persist user<br/>record]",
                "  E --> F[Return created<br/>user response]",
        )

    def render_login_sequence(self) -> str | None:
        endpoint = self._find_endpoint("login")
        if not endpoint:
            return None
        return self._mermaid_block(
                "sequenceDiagram",
                "  participant Client",
                "  participant Api as API",
                "  participant Auth as AuthSvc",
                "  participant Token as Token",
                "  participant Repo as Repo",
                "  participant DB as DB",
                f"  Client->>Api: {endpoint.http_method} /auth/login",
                "  Api->>Auth: login(request)",
                "  Auth->>Auth: validate payload",
                "  Auth->>Auth: authenticate user",
                "  Auth->>Auth: load user context",
                "  Auth->>Token: issue access + refresh",
                "  Auth->>Repo: save token",
                "  Repo->>DB: insert token row",
                "  DB-->>Repo: row stored",
                "  Repo-->>Auth: token persisted",
                "  Token-->>Auth: token pair ready",
                "  Auth-->>Api: login response",
                "  Api-->>Client: 200 OK",
        )

    def render_refresh_sequence(self) -> str | None:
        endpoint = self._find_endpoint("refresh")
        if not endpoint:
            return None
        return self._mermaid_block(
                "sequenceDiagram",
                "  participant Client",
                "  participant Api as API",
                "  participant Auth as AuthSvc",
                "  participant Token as Token",
                "  participant Repo as Repo",
                "  participant DB as DB",
                f"  Client->>Api: {endpoint.http_method} /auth/refresh",
                "  Api->>Auth: refresh(request)",
                "  Auth->>Auth: validate payload",
                "  Auth->>Repo: find token",
                "  Repo->>DB: select token row",
                "  DB-->>Repo: token row",
                "  Repo-->>Auth: current token",
                "  Auth->>Auth: validate token state",
                "  Auth->>Auth: resolve token owner",
                "  Auth->>Token: issue new access",
                "  Auth->>Repo: save replacement",
                "  Repo->>DB: update token state",
                "  DB-->>Repo: row updated",
                "  Token-->>Auth: new access ready",
                "  Auth-->>Api: refresh response",
                "  Api-->>Client: 200 OK",
        )

    def render_refresh_token_lifecycle(self) -> str | None:
        if not self._find_endpoint("login") or not self._find_endpoint("refresh"):
            return None
        return self._mermaid_block(
                "flowchart LR",
                "  Issue[Login issues<br/>token] --> Store[Persist token]",
                "  Store --> Use[Client sends<br/>token]",
                "  Use --> Validate[Validate token]",
                "  Validate --> Rotate[Generate<br/>replacement]",
                "  Rotate --> Revoke[Old token<br/>revoked]",
                "  Revoke --> Persist[Persist new<br/>token state]",
        )

    def render_rate_limit_flow(self) -> str:
        return self._mermaid_block(
                "flowchart TD",
                "  A[Incoming auth request] --> B{Path matches register/login/refresh}",
                "  B -- No --> C[Continue normal request handling]",
                "  B -- Yes --> D[Matching rate limit interceptor runs]",
                "  D --> E{Limit exceeded in active window}",
                "  E -- No --> F[Request proceeds to controller]",
                "  E -- Yes --> G[Request blocked and metric recorded]",
        )

    def _find_endpoint(self, keyword: str) -> EndpointInfo | None:
        keyword_lower = keyword.lower()
        for endpoint in self.facts.endpoints:
            if keyword_lower in endpoint.path.lower():
                return endpoint
        return None

    def _path_exists(self, suffix: str) -> bool:
        return any(path.as_posix().lower().endswith(suffix.lower()) for path in self.service.root.rglob("*") if path.is_file())

    def _has_auth_surface(self) -> bool:
        return any("/auth/" in endpoint.path for endpoint in self.facts.endpoints)

    def _path_prefix(self, path: str) -> str:
        parts = [part for part in path.split("/") if part]
        if len(parts) <= 1:
            return path
        return "/" + "/".join(parts[:-1])

    def _endpoint_purpose(self, endpoint: EndpointInfo) -> str:
        path = endpoint.path.lower()
        if "register" in path:
            return "Creates a new user account and returns the created user representation."
        if "login" in path:
            return "Authenticates a user and returns `tokenType`, `accessToken`, `refreshToken`, `expiresIn`, and the mapped user representation."
        if "refresh" in path:
            return "Validates the submitted refresh token, rotates refresh-token state, and returns `tokenType`, `accessToken`, `expiresIn`, and a replacement refresh token."
        if "logout" in path:
            return "Accepts a refresh token request body and logs the user out by revoking the submitted active refresh token."
        if endpoint.http_method.upper() == "GET" and path.endswith("/user/me"):
            return "Returns the authenticated active user's `id`, `email`, `username`, and `createdAt`; sensitive fields such as `password`, `deletedAt`, and related collections are omitted from the response DTO."
        if endpoint.http_method.upper() == "PATCH" and path.endswith("/user/me"):
            return "Updates the authenticated active user's profile fields and returns the updated `id`, `email`, `username`, and `createdAt` values from the response DTO."
        if endpoint.http_method.upper() == "PATCH" and path.endswith("/user/me/password"):
            return "Changes the authenticated active user's password after validating the current password and returns no response body."
        if endpoint.http_method.upper() == "DELETE" and path.endswith("/user/me"):
            return "Soft-deletes the authenticated active user by setting `deletedAt`, preserves the stored record, revokes active refresh tokens, and returns no response body."
        return "Exposes a code-backed service operation through the HTTP API."

    def _endpoint_success_status(self, endpoint: EndpointInfo) -> str:
        if "register" in endpoint.path.lower():
            return "201 Created"
        return "200 OK"

    def _render_endpoint_contract_table(self, endpoint: EndpointInfo) -> str:
        lines = [
            "| Contract Item | Value |",
            "| --- | --- |",
            f"| Success status | `{self._endpoint_success_status(endpoint)}` |",
            f"| Source API | `{endpoint.source_api}` |",
            f"| Request DTO | `{endpoint.request_type or 'N/A'}` |",
            f"| Response DTO | `{endpoint.response_type or 'N/A'}` |",
        ]
        return "\n".join(lines)

    def _render_endpoint_validation_table(self, endpoint: EndpointInfo) -> str:
        lines: list[str] = [
            "| Input | Rules |",
            "| --- | --- |",
        ]
        contract = self.facts.dto_contracts.get(endpoint.request_type or "")
        if contract and contract.field_constraints:
            for field in contract.field_constraints:
                rule_parts = list(field.rules)
                if field.normalization:
                    rule_parts.append(field.normalization)
                if rule_parts:
                    lines.append(f"| `{field.field_name}` | {'; '.join(rule_parts)} |")
        elif endpoint.request_type:
            lines.append(f"| `{endpoint.request_type}` | Request contract detected, but no field-level validation metadata was extracted. |")
        else:
            lines.append("| Request body | No request DTO is associated with this endpoint. |")

        if contract and contract.cross_field_rules:
            lines.append(f"| Cross-field checks | {'; '.join(contract.cross_field_rules)} |")
        return "\n".join(lines)

    def _render_endpoint_runtime_constraints(self, endpoint: EndpointInfo) -> str:
        return "\n".join(self._endpoint_runtime_constraints(endpoint, self.facts.dto_contracts.get(endpoint.request_type or "")))

    def _endpoint_runtime_constraints(self, endpoint: EndpointInfo, contract: DtoContract | None) -> list[str]:
        lines: list[str] = []
        path = endpoint.path.lower()
        if "register" in path:
            lines.append("- Service-layer checks: email and username are normalized and must remain unique before user creation proceeds.")
            lines.append("- Dedicated rate limiting: registration requests are intercepted through the register rate-limit configuration.")
        elif "login" in path:
            lines.append("- Service-layer checks: email is normalized before lookup and the password must satisfy the login password policy before credential matching.")
            lines.append("- Dedicated rate limiting: login requests are intercepted through the login rate-limit configuration.")
        elif "refresh" in path:
            lines.append("- Service-layer checks: the refresh token must be present, resolvable, not revoked, and still within its validity window before rotation succeeds.")
            lines.append("- Dedicated rate limiting: refresh requests are intercepted through the refresh rate-limit configuration.")
        if self._path_exists("error/handler/ErrorHandler.java"):
            lines.append("- Validation failures are aggregated by the centralized `ErrorHandler` instead of being returned ad hoc from each controller method.")
        return lines

    def _render_endpoint_error_table(self, endpoint: EndpointInfo) -> str:
        lines = [
            "| Status | When it happens |",
            "| --- | --- |",
            "| `400 Bad Request` | DTO validation fails, request JSON is malformed, or an auth-specific password format rule rejects the payload. |",
        ]
        path = endpoint.path.lower()
        if "register" in path:
            lines.append("| `409 Conflict` | Registration collides with an existing email or username, either during pre-checks or at the persistence layer. |")
        if "login" in path:
            lines.append("| `401 Unauthorized` | Email does not resolve to a user or the provided password does not match the stored hash. |")
        if "refresh" in path:
            lines.append("| `401 Unauthorized` | Refresh token is missing, invalid, revoked, expired, or otherwise rejected during refresh-token validation. |")
        if self._path_exists("config/RefreshRateLimitConfig.java") and any(token in path for token in ("register", "login", "refresh")):
            lines.append("| `429 Too Many Requests` | The endpoint-specific rate-limit interceptor blocks the request because the active window has been exceeded. |")
        if self._path_exists("error/handler/ErrorHandler.java"):
            lines.append("| Error payload shape | Centralized through `ErrorHandler`, which maps validation, auth, rate-limit, and generic failures to the API response contract. |")
        return "\n".join(lines)

    def _entity_purpose(self, class_name: str) -> str:
        lowered = class_name.lower()
        if "user" in lowered:
            return "Stores the primary user identity and credential state."
        if "refreshtoken" in lowered:
            return "Stores refresh tokens, expiry, and revocation state linked to a user."
        if "portfolio" in lowered:
            return "Groups assets and investment preferences owned by a user."
        if "asset" in lowered:
            return "Represents an asset position that belongs to a portfolio."
        return "Stores service state used by the implemented domain."

    def _describe_relation(self, relation) -> str:
        target = relation.target_entity
        join = relation.join_column or "mapped association"
        if relation.relation_type == "many-to-one":
            return f"- Many records point to `{target}` through `{join}`."
        if relation.relation_type == "one-to-many":
            return f"- One record owns a collection associated with `{target}`."
        return f"- `{relation.relation_type}` relation to `{target}` via `{join}`."

    def _entity_fields_table(self, entity) -> str:
        if not entity.fields:
            return "No scalar fields were parsed."
        lines = [
            "| Column | Type | Required | Meaning | Notes |",
            "| --- | --- | --- | --- | --- |",
        ]
        for field in entity.fields:
            lines.append(
                f"| `{field.name}` | `{field.type_name}` | {'No' if field.nullable else 'Yes'} | {self._field_meaning(entity.class_name, field.name)} | {self._field_notes(entity.class_name, field.name, field.type_name)} |"
            )
        return "\n".join(lines)

    def _field_meaning(self, entity_class_name: str, field_name: str) -> str:
        key = f"{entity_class_name.lower()}::{field_name.lower()}"
        meanings = {
            "userentity::email": "Primary email used to identify the user account.",
            "userentity::username": "Public-facing or login-friendly user name.",
            "userentity::password": "Stored password hash used during authentication.",
            "userentity::created_at": "Timestamp recording when the user record was created.",
            "refreshtokenentity::token": "Opaque refresh-token value persisted for token rotation.",
            "refreshtokenentity::expires_at": "Instant after which the refresh token is no longer valid.",
            "refreshtokenentity::revoked": "Flag indicating whether the refresh token can still be used.",
            "refreshtokenentity::created_at": "Timestamp recording when the refresh token was issued.",
            "portfolioentity::name": "Short portfolio name shown to the user.",
            "portfolioentity::description": "Optional free-text description of the portfolio.",
            "portfolioentity::base_currency": "Reference currency used to express portfolio values.",
            "portfolioentity::risk_profile": "Risk appetite classification linked to the portfolio.",
            "portfolioentity::investment_horizon": "Expected holding horizon for the portfolio strategy.",
            "portfolioentity::strategy_type": "Strategy style associated with the portfolio.",
            "portfolioentity::max_risk": "Optional cap on accepted portfolio risk.",
            "portfolioentity::created_at": "Timestamp recording when the portfolio was created.",
            "portfolioentity::updated_at": "Timestamp recording the most recent portfolio update.",
            "assetentity::symbol": "Ticker or symbol used to identify the asset.",
            "assetentity::asset_type": "Classification of the asset instrument.",
            "assetentity::quantity": "Position size currently held in the portfolio.",
            "assetentity::average_price": "Average acquisition price for the asset position.",
            "assetentity::currency": "Currency associated with the asset valuation.",
            "assetentity::created_at": "Timestamp recording when the asset record was created.",
            "assetentity::updated_at": "Timestamp recording the most recent asset update.",
        }
        return meanings.get(key, "Domain field used by the implemented persistence model.")

    def _field_notes(self, entity_class_name: str, field_name: str, type_name: str) -> str:
        field_lower = field_name.lower()
        type_lower = type_name.lower()
        notes: list[str] = []
        if field_lower.endswith("_at") or field_lower in {"created_at", "updated_at", "expires_at"}:
            notes.append("timestamp")
        if field_lower == "revoked":
            notes.append("lifecycle flag")
        if field_lower in {"email", "username", "token", "symbol", "name"}:
            notes.append("core identifier")
        if "enum" in type_lower:
            notes.append("enum value")
        if field_lower == "password":
            notes.append("sensitive credential data")
        if field_lower in {"base_currency", "currency"}:
            notes.append("currency context")
        return ", ".join(notes) if notes else "No special note."


class UserServiceRenderer(BaseServiceRenderer):
    pass


class DeterministicRenderer:
    def __init__(self, catalog: ServiceCatalog) -> None:
        self.catalog = catalog

    def render(self, doc_path: Path, facts_by_service: dict[str, ServiceFacts], target_docs: list[Path]) -> str | None:
        if doc_path == self.catalog.index_path:
            return self._render_index(target_docs)
        if doc_path == self.catalog.project_context_path:
            return None
        if doc_path == self.catalog.docs_root / "architecture" / "system.md":
            return self._render_system_page(facts_by_service)
        if doc_path == self.catalog.docs_root / "architecture" / "observability.md":
            return self._render_observability_page(facts_by_service)
        if doc_path == self.catalog.docs_root / "database" / "overview.md":
            return self._render_database_page(facts_by_service)
        service = self.catalog.get_service_for_doc(doc_path)
        if not service:
            return None
        facts = facts_by_service.get(service.name)
        if not facts:
            return None
        return self._get_service_renderer(service, facts).render(doc_path)

    def _get_service_renderer(self, service: ServiceDefinition, facts: ServiceFacts) -> BaseServiceRenderer:
        if service.name == "user-service":
            return UserServiceRenderer(self.catalog, facts)
        return BaseServiceRenderer(self.catalog, facts)

    def _render_index(self, target_docs: list[Path]) -> str:
        service_sections: list[str] = []
        for service in self.catalog.services:
            service_sections.append(
                "\n".join(
                    [
                        f"### {service.name}",
                        "",
                        '<div class="tyche-card-grid">',
                        f'<a class="tyche-card" href="services/{service.name}/readme/">',
                        '<span class="tyche-card__eyebrow">Entry Point</span>',
                        '<strong>README</strong>',
                        "<p>The service-level operational summary mirrored into the docs site.</p>",
                        "</a>",
                        f'<a class="tyche-card" href="services/{service.name}/overview/">',
                        '<span class="tyche-card__eyebrow">Service</span>',
                        '<strong>Overview</strong>',
                        "<p>Scope, responsibilities, and the current implementation shape.</p>",
                        "</a>",
                        f'<a class="tyche-card" href="services/{service.name}/api/">',
                        '<span class="tyche-card__eyebrow">Contracts</span>',
                        '<strong>API</strong>',
                        "<p>Endpoints, validation, error behavior, and request flow diagrams.</p>",
                        "</a>",
                        f'<a class="tyche-card" href="services/{service.name}/data-model/">',
                        '<span class="tyche-card__eyebrow">Persistence</span>',
                        '<strong>Data Model</strong>',
                        "<p>Entities, relationships, schema notes, and database structure.</p>",
                        "</a>",
                        f'<a class="tyche-card" href="services/{service.name}/observability/">',
                        '<span class="tyche-card__eyebrow">Operations</span>',
                        '<strong>Observability</strong>',
                        "<p>Dashboard intent, metric groups, and operational checks.</p>",
                        "</a>",
                        "</div>",
                    ]
                )
            )
        service_count = len(self.catalog.services)
        core_cards = "\n".join(
            [
                '<div class="tyche-card-grid tyche-card-grid--compact">',
                '<a class="tyche-card tyche-card--soft" href="project-context/">',
                '<span class="tyche-card__eyebrow">Context</span>',
                "<strong>Project Context</strong>",
                "<p>Stable repository context for agents and future documentation runs.</p>",
                "</a>",
                '<a class="tyche-card tyche-card--soft" href="architecture/system/">',
                '<span class="tyche-card__eyebrow">Architecture</span>',
                "<strong>System Architecture</strong>",
                "<p>Repository-level layering, interactions, and service boundaries.</p>",
                "</a>",
                '<a class="tyche-card tyche-card--soft" href="architecture/observability/">',
                '<span class="tyche-card__eyebrow">Operations</span>',
                "<strong>Observability Architecture</strong>",
                "<p>Shared Prometheus and Grafana flow, metric families, and repository config layout.</p>",
                "</a>",
                '<a class="tyche-card tyche-card--soft" href="database/overview/">',
                '<span class="tyche-card__eyebrow">Persistence</span>',
                "<strong>Database Overview</strong>",
                "<p>Consolidated schema view across implemented services.</p>",
                "</a>",
                "</div>",
            ]
        )
        quick_start = "\n".join(
            [
                '<div class="tyche-card-grid tyche-card-grid--compact">',
                '<a class="tyche-card tyche-card--action" href="project-context/">'
                "<strong>Understand the implemented scope</strong>"
                "<p>Start with repository context, then move into the service overview.</p>"
                "</a>" if any(service.name == "user-service" for service in self.catalog.services) else '<a class="tyche-card tyche-card--action" href="project-context/"><strong>Understand the implemented scope</strong><p>Start with repository context, then move into service overviews.</p></a>',
                '<a class="tyche-card tyche-card--action" href="services/user-service/api/">'
                "<strong>Review API contracts</strong>"
                "<p>Inspect the auth surface, validation rules, and request flows.</p>"
                "</a>" if any(service.name == "user-service" for service in self.catalog.services) else '<a class="tyche-card tyche-card--action" href="database/overview/"><strong>Review API contracts</strong><p>Open the service API pages available in this repository.</p></a>',
                '<a class="tyche-card tyche-card--action" href="database/overview/">'
                "<strong>Trace entities and schema</strong>"
                "<p>Cross-check database relationships against service-level data models.</p>"
                "</a>",
                '<a class="tyche-card tyche-card--action" href="reports/">'
                "<strong>Inspect generated reports</strong>"
                "<p>Follow automation history and branch-level execution summaries.</p>"
                "</a>",
                "</div>",
            ]
        )
        repo_snapshot = "\n".join(
            [
                '<div class="tyche-metric-row">',
                f'<div class="tyche-metric"><span class="tyche-metric__value">{service_count}</span><span class="tyche-metric__label">Implemented services</span></div>',
                '<div class="tyche-metric"><span class="tyche-metric__value">Compact</span><span class="tyche-metric__label">Documentation model</span></div>',
                '<div class="tyche-metric"><span class="tyche-metric__value">Stable</span><span class="tyche-metric__label">Project context page</span></div>',
                '<div class="tyche-metric"><span class="tyche-metric__value">Tracked</span><span class="tyche-metric__label">Generated report history</span></div>',
                "</div>",
            ]
        )
        reports_cards = "\n".join(
            [
                '<div class="tyche-card-grid tyche-card-grid--compact">',
                '<a class="tyche-card tyche-card--soft" href="reports/">',
                '<span class="tyche-card__eyebrow">Reports</span>',
                "<strong>Overview</strong>",
                "<p>Entry point for generated execution reports across the repository.</p>",
                "</a>",
                '<a class="tyche-card tyche-card--soft" href="reports/feature/">',
                '<span class="tyche-card__eyebrow">Feature Branches</span>',
                "<strong>Feature History</strong>",
                "<p>Grouped reports by feature branch folder and latest generated alias.</p>",
                "</a>",
                '<a class="tyche-card tyche-card--soft" href="reports/fix/">',
                '<span class="tyche-card__eyebrow">Fix Branches</span>',
                "<strong>Fix History</strong>",
                "<p>Reserved for bugfix and hotfix report histories when present.</p>",
                "</a>",
                "</div>",
            ]
        )
        return "\n".join(
            [
                "# Tyche Wealth Documentation",
                "",
                '<div class="tyche-hero">',
                '<span class="tyche-hero__eyebrow">Technical Documentation</span>',
                "<h2>Autogenerated documentation grounded in the current repository state.</h2>",
                "<p>This hub is generated from the codebase and supporting documentation workflow. It is meant to expose what is implemented today: service boundaries, persistence structure, runtime behavior, API contracts, and the execution reports that explain how each documentation run was produced.</p>",
                '<div class="tyche-hero__actions">',
                '<a class="md-button md-button--primary" href="project-context/">Open project context</a>',
                '<a class="md-button" href="services/user-service/overview/">Explore user-service</a>',
                "</div>",
                "</div>",
                "",
                "## Snapshot",
                "",
                repo_snapshot,
                "",
                "## Quick Start",
                "",
                quick_start,
                "",
                "## Core Pages",
                "",
                core_cards,
                "",
                "## Service Pages",
                "",
                *("\n\n".join(service_sections).splitlines() if service_sections else ["- No implemented services detected."]),
                "",
                "## Reports",
                "",
                reports_cards,
                "",
                "## Example Layout",
                "",
                '<div class="tyche-path-list">',
                '<code>reports/feature/TYCHE-10-implement-ci-pipeline/latest-generate_docs.md</code>',
                '<code>reports/feature/TYCHE-10-implement-ci-pipeline/2026-03-12-001920-generate_docs.md</code>',
                '<code>reports/fix/TYCHE-42-correct-rate-limit/latest-review_diagrams.md</code>',
                "</div>",
                "",
                "## Navigation Notes",
                "",
                "- Prefer these consolidated pages over recreating many small pages for the same area.",
                "- Treat `project-context.md` as stable context and the service pages as the evolving implementation view.",
                "- Use reports to understand what each script changed before deciding whether to keep or roll back a run.",
            ]
        ).strip() + "\n"

    def _render_system_page(self, facts_by_service: dict[str, ServiceFacts]) -> str:
        service_lines = "\n".join(f"- `{name}`" for name in sorted(facts_by_service)) or "- No implemented services detected."
        service_summary_sections: list[str] = []
        layer_notes = "\n".join(
            [
                "- Requests enter through controller interfaces and controller implementations.",
                "- Business orchestration lives in service and helper classes.",
                "- Persistence is handled through repositories, JPA entities, and schema-management files.",
                "- Cross-cutting concerns such as security and rate limiting are wired from configuration and web layers.",
            ]
        )
        diagrams: list[str] = []
        for service_name, facts in sorted(facts_by_service.items()):
            renderer = self._get_service_renderer(facts.service, facts)
            service_summary_sections.append(f"### {service_name}\n\n{renderer.render_system_summary()}")
            diagrams.append(f"### {service_name}\n\n{renderer.render_system_architecture()}")
        cross_cutting_sections = [
            f"### {service_name}\n\n{self._get_service_renderer(facts.service, facts).render_cross_cutting_summary()}"
            for service_name, facts in sorted(facts_by_service.items())
        ]
        evolution = "- The current repository is centered on the implemented services above. Any broader microservice picture should be treated as target architecture until more concrete services and integrations appear in code."
        return "\n".join(
            [
                "# System Architecture",
                "",
                "## Overview",
                "",
                "This page consolidates the backend architecture that is actually implemented today in the repository.",
                "",
                "## Repository Snapshot",
                "",
                "| Aspect | Current State |",
                "| --- | --- |",
                f"| Implemented services | `{len(facts_by_service)}` |",
                "| Dominant stack | Spring Boot, JPA, PostgreSQL, Liquibase |",
                "| Current API emphasis | Authentication-first surface in `user-service` |",
                "| Documentation stance | Describe implemented code, not conceptual microservices |",
                "",
                "## Implemented Components",
                "",
                service_lines,
                "",
                "## Service Snapshots",
                "",
                "\n\n".join(service_summary_sections) if service_summary_sections else "No service summaries were generated.",
                "",
                "## Layered Structure",
                "",
                layer_notes,
                "",
                "## Cross-Cutting Concerns",
                "",
                "\n\n".join(cross_cutting_sections) if cross_cutting_sections else "No cross-cutting summary was generated.",
                "",
                "## Interactions",
                "",
                "\n\n".join(diagrams) if diagrams else "No architecture diagrams were generated.",
                "",
                "## Evolution Notes",
                "",
                evolution,
                "",
            ]
        )

    def _render_database_page(self, facts_by_service: dict[str, ServiceFacts]) -> str:
        sections = []
        snapshot_lines = [
            "| Aspect | Current State |",
            "| --- | --- |",
            f"| Implemented services with persistence | `{len(facts_by_service)}` |",
            f"| Total detected entities | `{sum(len(facts.entities) for facts in facts_by_service.values())}` |",
            f"| Liquibase-backed services | `{sum(1 for facts in facts_by_service.values() if (facts.service.root / 'src' / 'main' / 'resources' / 'db.changelog').exists())}` |",
        ]
        for _, facts in sorted(facts_by_service.items()):
            renderer = self._get_service_renderer(facts.service, facts)
            sections.append(renderer.render_database_overview())
        return "\n".join(
            [
                "# Database Overview",
                "",
                "## Overview",
                "",
                "This page consolidates the implemented database model across the repository.",
                "",
                "## Snapshot",
                "",
                *snapshot_lines,
                "",
                "## Implemented Service Schemas",
                "",
                "\n\n".join(sections) if sections else "No service schemas were generated.",
                "",
                "## Constraints and Persistence Notes",
                "",
                "- Review nullability, unique constraints, token lifecycle flags, timestamps, and join columns in code before changing this page.",
                "- Prefer entities and changelogs over older Markdown when the two diverge.",
                "",
                "## Source of Truth",
                "",
                "- JPA entities and changelog files are the source of truth for this page.",
                "",
            ]
        )

    def _render_observability_page(self, facts_by_service: dict[str, ServiceFacts]) -> str:
        renderer = None
        if facts_by_service:
            first_facts = facts_by_service[sorted(facts_by_service.keys())[0]]
            renderer = self._get_service_renderer(first_facts.service, first_facts)
        diagram = renderer.render_observability_architecture() if renderer else "No observability diagram available."
        return "\n".join(
            [
                "# Observability Architecture",
                "",
                "## Overview",
                "",
                "This page documents the observability flow that is currently implemented in the repository and the role of Prometheus and Grafana in the local stack.",
                "",
                "## Repository Snapshot",
                "",
                "| Aspect | Current State |",
                "| --- | --- |",
                "| Metrics producer | `user-service` |",
                "| Metrics endpoint | `/actuator/prometheus` |",
                "| Metrics collector | Prometheus |",
                "| Dashboard layer | Grafana |",
                "| Grafana repository configuration | `observability/grafana/` |",
                "",
                "## Implemented Flow",
                "",
                "- `user-service` exposes Prometheus-formatted metrics through Spring Boot Actuator.",
                "- Prometheus scrapes the exposed metrics endpoint and stores the resulting time-series locally.",
                "- Grafana uses Prometheus as its datasource and renders dashboard panels for operational inspection.",
                "",
                "## Interaction Diagram",
                "",
                diagram,
                "",
                "## Configuration Layout",
                "",
                "| Path | Purpose |",
                "| --- | --- |",
                "| `observability/grafana/provisioning/datasources/prometheus.yml` | Grafana datasource provisioning for Prometheus. |",
                "| `observability/grafana/provisioning/dashboards/dashboards.yml` | Grafana dashboard provisioning configuration. |",
                "| `observability/grafana/dashboards/tyche-user-service-overview.json` | Initial dashboard definition for `user-service`. |",
                "",
                "## Metrics Families",
                "",
                "| Metrics family | What it covers |",
                "| --- | --- |",
                "| `tyche_auth_*` | Auth-domain requests, outcomes, token lifecycle, and rate-limiting signals. |",
                "| `tyche_user_*` | User-domain requests, success outcomes, and domain-specific error signals. |",
                "| `http_server_requests_*` | Endpoint traffic, latency, and response status observations. |",
                "| `jvm_*` | JVM memory, threads, and runtime state. |",
                "| `jdbc_*` | Datasource and connection-pool state. |",
                "",
                "## Notes",
                "",
                "- The current repository contains one implemented service, so the observability flow is presently centered on `user-service`.",
                "- Dashboard panels are operational views over Prometheus data and should be interpreted together with the selected time range and generated traffic.",
                "",
            ]
        )

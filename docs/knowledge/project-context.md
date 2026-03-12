# Project Context

## Purpose

Tyche Wealth is a backend-focused project for a wealth management platform. At the current state of the repository, the main implemented component is `user-service`, a Spring Boot service that centralizes:

- user registration and login
- refresh token management
- portfolio modeling
- asset modeling

This document is intended to be consumed by an AI agent that generates or updates technical documentation. Its main goal is to give enough project context to produce accurate docs without inventing architecture or features that are not present in the codebase.

## Current Repository Scope

Top-level structure:

- `docs/`: project documentation in Markdown
- `user-service/`: Java 21 + Spring Boot backend service
- `scripts/`: automation helpers for documentation generation

Important constraint:

- Although the documentation structure suggests a microservices architecture, the repository currently contains one implemented backend service: `user-service`.
- Documentation generators should distinguish between "implemented in code" and "planned or conceptual in docs".

## Main Technology Stack

The `user-service` uses:

- Java 21
- Spring Boot 3.2.5
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Actuator
- springdoc OpenAPI / Swagger UI
- Liquibase
- PostgreSQL
- Lombok
- MapStruct
- Spring Security Crypto (`BCryptPasswordEncoder`)
- JJWT for JWT handling
- Caffeine for rate-limit support

## Implemented Domain Model

The main persisted entities currently present in code are:

- `UserEntity`: user account with `email`, `username`, `password`, `createdAt`
- `RefreshTokenEntity`: refresh token linked to a user, with expiration and revocation state
- `PortfolioEntity`: investment portfolio linked to a user
- `AssetEntity`: asset position linked to a portfolio

Current relationship model:

- one user -> many portfolios
- one user -> many refresh tokens
- one portfolio -> many assets

## API Surface Implemented in Code

The explicit controller contract implemented today is authentication-focused.

Base path:

- `/tyche-wealth/user-service/v1`

Implemented auth endpoints:

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`

Important constraint for documentation agents:

- Do not document CRUD endpoints for users, portfolios, or assets as implemented APIs unless those controllers exist in code.
- If documenting planned APIs from `docs/`, mark them as planned, conceptual, or pending implementation.

## Configuration and Runtime Assumptions

Main runtime configuration lives in:

- `user-service/src/main/resources/application.properties`
- optional local overrides in `user-service/application-local.properties`

Relevant environment/config variables:

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `JWT_SECRET`
- `JWT_ACCESS_TOKEN_TTL_SECONDS`
- `JWT_REFRESH_TOKEN_TTL_SECONDS`
- `AUTH_REGISTER_RATE_LIMIT_MAX_REQUESTS`
- `AUTH_REGISTER_RATE_LIMIT_WINDOW_SECONDS`
- `AUTH_LOGIN_RATE_LIMIT_MAX_REQUESTS`
- `AUTH_LOGIN_RATE_LIMIT_WINDOW_SECONDS`
- `AUTH_REFRESH_RATE_LIMIT_MAX_REQUESTS`
- `AUTH_REFRESH_RATE_LIMIT_WINDOW_SECONDS`

Default local port:

- `8080`

Useful local URLs when the service is running:

- `http://localhost:8080/swagger-ui/index.html`
- `http://localhost:8080/v3/api-docs`
- `http://localhost:8080/actuator/health`

## Persistence and Schema Source of Truth

Database schema evolution is managed with Liquibase.

Primary changelog root:

- `user-service/src/main/resources/db.changelog/changelog-master.xml`

Feature-specific changelog areas:

- `user-changelog`
- `refresh-token-changelog`
- `portfolio-changelog`
- `asset-changelog`

Rule for documentation generation:

- For database documentation, prefer Liquibase changelogs and JPA entities as the source of truth.
- If a Markdown document disagrees with code or changelogs, code and changelogs win.

## Package-Level Mental Model

Relevant package responsibilities in `user-service/src/main/java/com/tychewealth`:

- `controller`: API contracts
- `controller.impl`: REST controller implementations
- `service`: service interfaces
- `service.impl`: service implementations
- `service.helper`: auth workflow decomposition helpers
- `service.token`: token payload structures
- `service.monitoring`: auth metrics support
- `entity`: JPA entities
- `repository`: Spring Data repositories
- `dto`: request/response DTOs
- `mapper`: MapStruct mappers
- `config`: Spring configuration
- `web`: interceptors, including rate limiting
- `error`: exception and error response handling
- `constants`, `enums`, `utils`: shared support code

## Testing Status

There is test coverage for:

- repository behavior
- mapper behavior
- auth validation helpers
- rate-limit interceptors
- auth controller integration flows

Rule for documentation generation:

- When describing tested behavior, rely on test classes only as supporting evidence.
- Use production code as the primary source of truth.

## Documentation Structure Already Present

The `docs/` folder is organized by topic:

- `overview/`
- `architecture/`
- `authentication/`
- `services/`
- `database/`
- `flows/`
- `api/`
- `diagrams/`
- `development/`

Some existing pages are placeholders with `TODO`. Documentation automation should prefer updating those pages over creating redundant alternatives when the topic already exists.

## Rules for an AI Documentation Generator

### What the agent should do

- Read code before writing architecture claims.
- Treat `user-service` as the only implemented service unless additional services appear in the repository.
- Use entities, controllers, DTOs, repositories, and Liquibase changelogs to derive docs.
- Reuse existing pages in `docs/` when a matching topic already exists.
- Mark inferred or planned content explicitly.
- Generate or refresh diagrams from time to time when they add clarity or when the codebase changes significantly.
- Use different diagram types when appropriate, not just one format.
- Keep service-level `README` files updated when implementation, setup steps, endpoints, or operational assumptions change.
- Generate a final change report after each documentation update cycle.

### What the agent should not do

- Do not assume the full microservices platform is already implemented.
- Do not invent endpoints, tables, services, events, or integrations.
- Do not present conceptual docs as if they were already deployed or coded.
- Do not use old Markdown content as a stronger source of truth than the current codebase.

## Recommended Documentation Priority

If documentation is generated incrementally, the recommended order is:

1. `overview/project-overview.md`
2. `overview/system-architecture.md`
3. `services/user-service/overview.md`
4. `services/user-service/endpoints.md`
5. `services/user-service/data-model.md`
6. `api/authentication-api.md`
7. `database/schema.md`
8. auth and business flow documents
9. diagrams that reflect the current implemented system

## Diagram Generation Guidance

The documentation agent should generate diagrams periodically, especially after meaningful changes in API surface, data model, authentication flows, or repository structure.

Recommended diagram types:

- high-level system architecture diagrams
- service/module boundary diagrams
- authentication sequence diagrams
- request flow diagrams
- entity relationship or data model diagrams
- package or component interaction diagrams
- database schema diagrams

Rules for diagrams:

- Diagrams must reflect implemented code unless explicitly labeled as conceptual.
- Prefer simple diagrams with a clear single purpose over large mixed diagrams.
- Update existing diagram documents in `docs/diagrams/` when the topic already exists.
- If a diagram is inferred from documentation rather than code, mark it clearly as conceptual.

## README Maintenance Guidance

Besides `docs/`, the documentation agent should also review and update each microservice `README` when needed.

Typical triggers for a `README` update:

- setup or run commands change
- environment variables change
- local dependencies change
- exposed endpoints or URLs change
- new operational notes become relevant
- the documented scope of the service no longer matches the code

Rules for `README` maintenance:

- Keep `README` files concise and practical.
- Prefer implementation-backed instructions over aspirational descriptions.
- Align service `README` files with the current codebase and with `docs/`.
- If the repository only contains one implemented service, update that service `README` rather than describing non-existent services as active.

## Change Report Guidance

At the end of each documentation generation or update cycle, the agent should create a human-readable report that explains what changed.

Recommended location:

- keep all execution reports under a common folder inside `docs/`
- use `docs/knowledge/reports/<branch-name>/` as the default structure
- this keeps reports separated from architecture, API, and service documentation pages

Recommended filename patterns:

- `latest.md` for the latest branch summary
- timestamped files such as `2026-03-11-153000.md` for historical runs

The report should include:

- execution date and time
- git branch name
- files created
- files updated
- files intentionally skipped
- short explanation of each relevant change
- whether content was derived from code, inferred, or left as conceptual
- pending gaps or follow-up recommendations

Rules for the change report:

- Keep it readable for humans first.
- Summarize meaningful changes, not every trivial wording tweak.
- Be explicit when a document was updated to match code reality.
- Avoid mixing reports with normal documentation pages outside `docs/knowledge/reports/`.
- If no branch name is available, fall back to `docs/knowledge/reports/_unknown-branch/`.

## Canonical Sources by Topic

- Product/repository scope: repository root + folder structure
- Implemented APIs: `controller/`, `controller.impl/`, DTOs, OpenAPI output
- Business behavior: `service/`, `service.impl/`, `service.helper/`
- Data model: `entity/`, `repository/`, Liquibase changelogs
- Runtime configuration: `application.properties`, optional local properties
- Operational constraints: interceptors, config classes, security, tests

## Short Summary for Agents

If you need a compact prompt seed:

> Tyche Wealth currently contains a documented backend project whose implemented code is centered on a Spring Boot `user-service`. The service handles auth, refresh tokens, portfolios, and assets. Treat Java code and Liquibase changelogs as the source of truth, and mark any broader platform or microservice content as planned unless it exists in the repository.

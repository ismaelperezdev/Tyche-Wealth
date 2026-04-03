# Micros Playbook

## Purpose

This document captures the working conventions inferred from the implemented microservices in this repository. It is intended as fast context for future development work so new changes follow the existing engineering style instead of introducing a different one.

## Current Scope

The implemented backend services currently visible in the repository are:

- `user-service`
- `portfolio-service`

Both follow a shared Spring Boot service template and diverge mainly where domain rules require it.

## Core Development Style

- Keep controllers thin.
- Put orchestration in services.
- Extract domain validation and workflow rules into dedicated helpers.
- Keep persistence concerns in repositories, entities, and Liquibase changelogs.
- Treat DTOs and mappers as explicit API boundaries.
- Prefer implementation-backed documentation over aspirational architecture.

## Service Shape

The standard service shape repeated across the implemented micros is:

- `controller`: API contracts
- `controller.impl`: HTTP entrypoints
- `service`: service interfaces
- `service.impl`: orchestration logic
- `service.helper`: domain and validation helpers
- `repository`: persistence access
- `entity`: JPA model
- `dto`: request and response contracts
- `mapper`: MapStruct mapping layer
- `config`: Spring wiring
- `error`: exception and error response translation

## Controller Conventions

- Controllers are intentionally thin and mostly delegate.
- Request validation is performed at the HTTP boundary with `@Valid`.
- Logging happens at request start and success boundaries.
- Response codes are explicit and aligned with business intent.
- Sensitive auth endpoints may add `Cache-Control: no-store` and `Pragma: no-cache`.

## Service And Helper Conventions

- Services coordinate workflows instead of owning every rule directly.
- Helpers are used to isolate validation, domain checks, and reusable business steps.
- Persistence conflicts are translated into domain-specific exceptions when possible.
- Transactions are declared at the service layer and tightened for write paths when needed.

## Security Conventions

- JWT authentication is implemented through a shared `JwtAuthenticationFilter` pattern.
- Security configuration is modularized with:
  - `SecurityCommonConfig`
  - `ApplicationSecurityConfig`
  - `PrometheusSecurityConfig`
- Authentication context is represented by the authenticated user id.
- Security and token validation are treated as part of the service contract, not as infrastructure noise.

## Rate Limiting And Operations

- Rate limiting is a first-class concern.
- Redis-backed rate-limit storage is part of the expected runtime model.
- Store failures are handled explicitly instead of being ignored silently.
- Observability is built in through Actuator, Micrometer, Prometheus, and Grafana.

## Error Handling Conventions

- Services translate domain failures into typed exceptions.
- Global `ErrorHandler` classes convert failures into a stable HTTP error contract.
- Error responses consistently expose:
  - `code`
  - `type`
  - `description`
- Validation and malformed payloads are tested as part of the API contract.

## Testing Style

The preferred testing style in this repository is behavior-focused and integration-heavy.

- Use `@SpringBootTest` and `MockMvc` for endpoint-level verification.
- Verify HTTP status, payload shape, and persistence side effects together.
- Cover unhappy paths, not only happy paths.
- Use test helpers, builders, constants, and fixtures to avoid noisy setup duplication.
- Treat metrics, throttling, and auth flows as testable behavior when they are part of the contract.

## Documentation Style

- Read code before making architectural claims.
- Treat the implemented repository state as the source of truth.
- Prefer updating existing docs over creating parallel explanations.
- Keep service documentation practical and close to runtime reality.
- Distinguish clearly between implemented behavior and planned expansion.

## Practical Guidance For Future Changes

- When adding a new endpoint, mirror the existing controller -> service -> helper -> repository flow.
- When adding business rules, prefer a dedicated helper over expanding controllers or repositories.
- When adding persistence behavior, align entities, repositories, Liquibase, and tests together.
- When changing public behavior, update tests and docs in the same pass.
- When adding a new microservice, start from the existing template instead of inventing a new structure.

## Known Repository Drift

There is some drift between repo reality and older generated documentation.

- `portfolio-service` exists in code, but some context docs still describe `user-service` as the only implemented backend service.
- `ErrorHandler` behavior is conceptually aligned across services, but implementation details differ and may be worth standardizing later.
- Rate-limiting patterns are similar across services, but not fully unified yet.

## Short Summary

Tyche Wealth currently follows a disciplined Spring Boot microservice style: thin controllers, orchestrating services, helper-driven domain logic, explicit error contracts, serious integration testing, and documentation that should stay grounded in implemented code.

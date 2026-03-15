# User Service Overview

## Overview

`user-service` is an implemented Spring Boot service in this repository. The configured application name is `user-service` and the default local port is `8080`. At the current repository state it is one of `1` implemented backend services detected in code.

## At a Glance

| Aspect | Current State |
| --- | --- |
| Service name | `user-service` |
| Spring application name | `user-service` |
| Default local port | `8080` |
| Detected HTTP endpoints | `6` |
| Detected persisted entities | `4` |
| Implementation slices | `config`, `controller`, `dto`, `entity`, `helper`, `mapper`, `repository`, `service`, `web` |

## Responsibilities

- Exposes the implemented authentication API for register, login, and refresh operations.
- Coordinates validation, token generation, refresh-token lifecycle handling, and persistence updates.
- Stores user-facing auth state and related portfolio or asset ownership data through JPA entities and repositories.

## Implemented Scope

- Detected HTTP endpoints: `6`
- Detected persisted entities: `4`
- Detected implementation slices: `config`, `controller`, `dto`, `entity`, `helper`, `mapper`, `repository`, `service`, `web`
- The implemented surface should be read from controllers, services, helpers, repositories, entities, and configuration classes rather than from older Markdown pages.

## Main Components

- API contracts and controller implementations define the externally visible HTTP surface.
- Service and helper classes contain orchestration, validation, token, and domain logic.
- Repositories and entities define persisted state and object relationships.
- Configuration and web classes provide security, rate limiting, and request interception support.

## Security and Operational Notes

- Password handling is centralized in `SecurityConfig` through a `BCryptPasswordEncoder`, so raw credentials are not persisted directly from controller input.
- Access tokens are signed as JWTs with `HS256`; the signing secret is injected from `app.auth.jwt.secret`, and the configured access-token TTL is `${JWT_ACCESS_TOKEN_TTL_SECONDS:900}` seconds.
- Refresh tokens are generated with `SecureRandom`, encoded for transport, persisted in the `refresh_tokens` table, and revoked or rotated on use; the configured refresh-token TTL is `${JWT_REFRESH_TOKEN_TTL_SECONDS:1209600}` seconds.
- Register, login, and refresh routes are protected by dedicated MVC interceptors. Throttling is keyed by `HttpServletRequest.getRemoteAddr()` and enforced before the request reaches controller logic.
- Registration is limited to `${AUTH_REGISTER_RATE_LIMIT_MAX_REQUESTS:5}` requests per `${AUTH_REGISTER_RATE_LIMIT_WINDOW_SECONDS:300}` seconds per client address.
- Login is limited to `${AUTH_LOGIN_RATE_LIMIT_MAX_REQUESTS:10}` requests per `${AUTH_LOGIN_RATE_LIMIT_WINDOW_SECONDS:60}` seconds per client address.
- Refresh is limited to `${AUTH_REFRESH_RATE_LIMIT_MAX_REQUESTS:10}` requests per `${AUTH_REFRESH_RATE_LIMIT_WINDOW_SECONDS:60}` seconds per client address.
- Metrics are emitted for auth requests, successes, failures, invalid credentials, token issuance, token revocation, and rate-limited outcomes, which makes abuse patterns and auth regressions observable.
- Secrets are expected from environment variables or local-only property imports, which keeps JWT secrets and datasource credentials out of committed defaults.

## Related Documentation

- `docs/knowledge/services/user-service/api.md`
- `docs/knowledge/services/user-service/data-model.md`
- `docs/knowledge/services/user-service/runtime.md`
- `docs/knowledge/services/user-service/observability.md`
- `docs/knowledge/project-context.md`

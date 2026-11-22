# Spike: Internal Authentication with OAuth2 Client Credentials in user-service

Spanish version: [internal-service-authentication-spike.es.md](internal-service-authentication-spike.es.md).

## Investigation

### Objective and scope decision

Centralize service-token issuance in `user-service` through Spring Authorization Server without migrating or replacing current user authentication. The initial scope is Portfolio → Market Data Service (MDS). MDS must reject user tokens on its internal endpoints.

This document resolves the security decision deferred in `docs/spikes/market-data-price-history-spike.md`. It does not reopen pagination, batching, result limits, or price composition.

The investigation is based on source inspection, managed dependencies, and Spring documentation. No Authorization Server proof of concept has been implemented or compiled.

### Existing implementation

- `services/portfolio-service/src/main/java/com/tychewealth/controller/PortfolioMarketDataApi.java` and `controller/impl/PortfolioMarketDataApiController.java` receive the user principal and the `Authorization` header.
- `services/portfolio-service/src/main/java/com/tychewealth/service/PortfolioMarketDataService.java` and `service/impl/PortfolioMarketDataServiceImpl.java` propagate that header. The implementation validates selection and ownership, resolves symbols, and forwards the user token through its private `accessToken` method.
- `clients/market-data-client/src/main/java/com/tychewealth/marketdataclient/MarketDataClient.java` already accepts an access token in `getLatestPrices` and `getPriceHistory`, sending it to `/internal/prices/latest` and `/internal/prices/history`.
- `services/market-data-service/src/main/java/com/tychewealth/security/config/ApplicationSecurityConfig.java` uses `JwtAuthenticationFilter` and requires authentication, but not machine identity or scopes.
- In MDS, `security/token/AccessTokenSupport.java`, `TokenValidator.java`, and `TokenStateSupport.java` validate HMAC user tokens, numeric subjects, and Redis revocation state.
- In `user-service`, `config/security/PrometheusSecurityConfig.java` has order 1 and `ApplicationSecurityConfig.java` order 2. Existing user APIs use a custom JWT filter and retain their authentication contract.

The `/internal` prefix is not an authentication boundary. Currently, a valid user token can authenticate an MDS price query.

### Compatibility and future context

The root `pom.xml` specifies Java 21 and Spring Boot 3.5.14. Its BOM manages Spring Security 6.5.10 and Spring Authorization Server 1.5.7. The starter can be added only to the user-service POM without pinning another version or upgrading Boot. Versions must be rechecked during implementation if the project changes.

A future RAG service is expected to research Internet sources, autonomously generate and store reports, and expose them for internal Portfolio queries. Production is asynchronous; querying an available report can use synchronous HTTP. It requires neither Portfolio callbacks nor public endpoints. A transaction service is also being considered, but its flow and scope are undefined. Neither is implemented by this spike.

## Problems and Constraints

1. **User compatibility:** preserve existing login, refresh, logout, revocation, and numeric principals. A machine client is not a `UserEntity` account.
2. **Isolated chains:** Authorization Server needs a higher-priority chain limited by its endpoint matcher. OAuth endpoints do not inherit existing APIs' MVC prefixes.
3. **Filter registration:** `services/user-service/src/main/java/com/tychewealth/config/security/SecurityCommonConfig.java` exposes `JwtAuthenticationFilter` as a bean without disabling automatic servlet-filter registration. Restrict it to the user chain. MDS already demonstrates `FilterRegistrationBean` in its `security/config/SecurityCommonConfig.java`.
4. **Separate trust:** do not reuse `JWT_SECRET`. Machine tokens require asymmetric signing and explicit issuer, audience, identity, and scope policies. A valid signature alone is insufficient.
5. **Persistence:** PostgreSQL and Liquibase manage the schema. `services/user-service/src/main/resources/db.changelog/changelog-master.xml` includes users, refresh tokens, and devices, not OAuth clients. Do not reuse those tables for machine identities.
6. **Keys and secrets:** durable keys outside Git, public-only JWKS, and rotation using `kid`. Do not generate fresh production keys on every startup. Protect OAuth credentials in transit using TLS as well.
7. **Availability:** user-service is already critical; it will also be required to acquire and renew internal tokens. MDS validates current tokens locally. JWKS availability, caching, and key changes affect that independence. Never fall back to user tokens.
8. **Errors:** preserve OAuth protocol errors on the token endpoint and MDS's JSON contract on its APIs. Do not represent issuer downtime as a requirement for the user to log in again.
9. **Abuse protection:** `services/user-service/src/main/java/com/tychewealth/config/AuthRateLimitConfig.java` registers MVC interceptors. Do not assume they cover `/oauth2/token`, which security filters process.
10. **Realistic tests:** some user tests use test-specific security configuration. Add production-chain tests and an embedded servlet server to detect filter-registration issues.
11. **Coordinated deployment:** current Portfolio sends user tokens; proposed MDS rejects them. Separate stories do not imply independently compatible activations.
12. **Authorization boundary:** Portfolio retains asset ownership and selection checks. Service identity does not implicitly grant access to every user's private data. Future private reports and transactions require their own policies.
13. **Networking:** authentication does not replace exposure restrictions. A RAG service with outbound Internet connections needs no public API. Its internal credentials must not be sent to external pages or included in prompts.

## Possible Options

### Option: Static opaque token per consumer

#### Approach

Portfolio presents its own random credential, which MDS validates.

#### Expected Changes

Secret configuration and machine authentication in both services, replacing user-token forwarding.

#### Advantages

Smallest initial implementation, without an issuer or token renewal; appropriate for few relationships.

#### Disadvantages

Reusable credential until rotation, coordinated distribution, and no standard centralized issuance model as selected for the platform.

### Option: Short-lived JWT signed by Portfolio

#### Approach

Portfolio signs with its private key; MDS verifies using a public key.

#### Expected Changes

Token generation in Portfolio and asymmetric validation in MDS.

#### Advantages

No additional deployed issuer and local validation; the recipient cannot sign with the public key.

#### Disadvantages

Distributes issuance among business services and requires a custom trust model. Conflicts with centralizing authentication in user-service. Short expiration limits a stolen token but does not resolve private-key theft.

### Option: Standard Authorization Server in user-service

#### Approach

Introduce Spring Authorization Server for machine clients while preserving existing user authentication.

#### Expected Changes

OAuth chain, configuration-backed in-memory clients, asymmetric signing and JWKS; Portfolio as OAuth2 Client and MDS as Resource Server.

#### Advantages

Centralizes identity where authentication already exists, uses a standard protocol, and avoids another deployment. Does not require user migration.

#### Disadvantages

Shares operational and security impact and requires maintaining keys, credentials, permissions, persistence, and isolation tests. The framework alone is not a complete identity-administration console.

### Option: Separate provider such as Keycloak

#### Approach

An external or self-hosted provider issues machine tokens; current user login remains initially.

#### Expected Changes

Provider deployment and storage, client/resource registration, and OAuth integration of consumers and recipients.

#### Advantages

Built-in administration, federation, and SSO if required; independent deployment.

#### Disadvantages

Another operational component and two identity systems until users migrate. Its additional capabilities are not current requirements.

### Option: New identity microservice

#### Approach

Create a dedicated service using a standard Authorization Server.

#### Expected Changes

New module, deployment, storage, and monitoring in addition to OAuth integrations.

#### Advantages

Operational isolation and independent evolution.

#### Disadvantages

More infrastructure without a current isolation requirement. Moving user login as well would be a different scope.

### Option: Custom issuance endpoint in user-service

#### Approach

Validate credentials and issue JWTs using custom code.

#### Expected Changes

Implement machine authentication, issuance, permissions, errors, and rotation.

#### Advantages

May reduce initial integration for a minimal use case.

#### Disadvantages

Does not automatically constitute OAuth2 Client Credentials and duplicates framework responsibilities. Not selected for the reuse objective.

### Option: Shared internal-security libraries

#### Approach

Complement the solution with a possible `internal-security-support` for common validators and a possible `internal-client-support` for shared OAuth acquisition.

#### Expected Changes

Extract only stable, repeated code. Keep chains, routes, expected audiences, permissions, and errors in each service. Do not automatically install a global chain or wrap all of Spring Security.

#### Advantages

Prevents technical-contract divergence and supports reuse across projects.

#### Disadvantages

Adds shared APIs and compatibility responsibilities before duplication is confirmed. Two destinations within Portfolio are not two independent consumers.

Retained as an optional evolution: evaluate shared validation with a second recipient, and shared acquisition with a second consuming microservice. Not required for the initial implementation and does not reduce its initial estimate.

### Option: mTLS or workload identity

#### Approach

Authenticate services through certificates, replacing or complementing tokens.

#### Expected Changes

Certificate issuance, distribution, and renewal, plus operation-level authorization where needed.

#### Advantages

Connection-level identity and transport protection.

#### Disadvantages

Operational complexity unless the platform already provides it. Do not introduce a service mesh solely for this integration.

## Chosen Solution

### Decision

Use **Spring Authorization Server inside user-service for Client Credentials**, preserving user authentication. Portfolio requests tokens; it does not issue them or grant permissions. MDS authenticates services and authorizes its operations.

Accept shared deployment and criticality in user-service in exchange for avoiding another component. Reconsider separation for concrete isolation requirements, not microservice count. Future reuse is a benefit, not a reason to build a generic framework now.

### Detailed Design

#### Existing Classes to Modify

**`ApplicationSecurityConfig` — user-service**  
Path: `services/user-service/src/main/java/com/tychewealth/config/security/ApplicationSecurityConfig.java`

Keep the user chain after the OAuth chain. Do not change principals or make user APIs accept machine tokens.

**`SecurityCommonConfig` — user-service**  
Path: `services/user-service/src/main/java/com/tychewealth/config/security/SecurityCommonConfig.java`

Disable servlet registration of the user JWT filter and retain explicit insertion. Integrate OAuth secret encoding without changing user-password formats or conflicting with existing BCrypt configuration.

**Configuration and persistence — user-service**  
Paths: `services/user-service/pom.xml`, `src/main/resources/application.properties`, and `src/main/resources/db.changelog/changelog-master.xml`, relative to the module.

Add the starter only to this module. Introduce the standard JDBC registered-client repository and migrations based on the actual framework-version schema, adapted and tested for PostgreSQL. Do not add consent persistence for client-credentials-only flows without a real requirement.

**`PortfolioMarketDataApi`, `PortfolioMarketDataApiController`, `PortfolioMarketDataService`, `PortfolioMarketDataServiceImpl`**  
Paths relative to `services/portfolio-service/src/main/java/com/tychewealth/`: `controller/PortfolioMarketDataApi.java`, `controller/impl/PortfolioMarketDataApiController.java`, `service/PortfolioMarketDataService.java`, `service/impl/PortfolioMarketDataServiceImpl.java`.

Remove `authorizationHeader` from forwarding-related signatures and remove the private `accessToken` method. The inbound filter still requires a user JWT. Preserve routes, DTOs, ownership validation, temporal filtering, and limits. Acquire machine tokens after validation, asset resolution, and early returns.

**`MarketDataClient` — no changes expected**  
Path: `clients/market-data-client/src/main/java/com/tychewealth/marketdataclient/MarketDataClient.java`.

Reuse the existing token parameter. Do not add OAuth to this library. Do not create `UserServiceClient`: Spring OAuth2 Client already implements token requests. Such a business client would only be justified for user APIs other than OAuth.

**`ApplicationSecurityConfig` and `SecurityCommonConfig` — MDS**  
Paths: `services/market-data-service/src/main/java/com/tychewealth/security/config/ApplicationSecurityConfig.java` and `SecurityCommonConfig.java` in the same package.

Use OAuth2 Resource Server for price routes. Connect existing JSON handlers to bearer processing as well. Preserve Prometheus and health behavior and explicitly define unmatched-route policy.

Remove `security/JwtAuthenticationFilter.java` and user-authentication components under `security/token/` when no consumers remain. Do not leave obsolete beans requiring `JWT_SECRET` or Redis blacklisting. This does not remove Redis price storage.

**Portfolio and MDS configuration**

Add `spring-boot-starter-oauth2-client` and `spring-boot-starter-oauth2-resource-server` only to their respective POMs. Configure OAuth registration, token URI, credentials, issuer, and JWKS by environment without altering Portfolio's inbound user authentication.

#### New Classes to Create

Names are illustrative; single-implementation interfaces are not required.

**`AuthorizationServerConfig`**  
Suggested path: `services/user-service/src/main/java/com/tychewealth/config/security/AuthorizationServerConfig.java`.

Defines the higher-priority OAuth chain, limited by the server endpoint matcher, and standard components. Does not enable browser login, OIDC, dynamic registration, or user grants for this scope.

**`ServiceTokenClaimsCustomizer`**  
Suggested path: `services/user-service/src/main/java/com/tychewealth/service/token/ServiceTokenClaimsCustomizer.java`.

Standard extension assigning audience and machine identity for Client Credentials. Restricts permissions to those configured for the client and does not copy arbitrary request claims. Clients cannot freely select their recipients. The customizer remains a bean in `InternalServiceTokenServerConfig`: it reads the non-empty audience list from the in-memory registered client and checks it against the configured audience allowlist. Invalid or missing client configuration rejects issuance with `server_error`. Request parameters cannot override this policy.

**Configuration-backed client registrations**
Configured under `app.oauth2.authorization-server.clients` in `services/user-service/src/main/resources/application.properties` or external deployment configuration.

At startup, `InternalServiceTokenServerConfig` builds an `InMemoryRegisteredClientRepository` from configuration. The configured clients, scopes, audiences, and token lifetimes are rebuilt on each restart; there is no OAuth client table or runtime database administration. Client secrets are supplied through environment/secret management, encoded with the service `PasswordEncoder`, and held only as hashes in the in-memory registrations. Never commit production secrets.

**`InternalAccessTokenProvider`**  
Portfolio-local integration over the shared token-client component.

Configures `AuthorizedClientServiceOAuth2AuthorizedClientManager`, the stable service identity, and the registration for MDS. Spring manages reuse and reacquisition after expiration; do not implement custom HTTP protocol or caching by default.

**`InternalTokenValidators`**, if additional validators are needed  
Shared validation component configurable by each resource server.

Centralizes only shared internal-token contract rules not covered by the standard Resource Server. Each service's route and permission policy remains local to that microservice.

**Shared service-to-service authentication library**

It provides independent capabilities for OAuth2 clients (token acquisition and reuse) and resource servers (signature/JWKS validation and shared token-contract rules). Each microservice includes only the side it needs and supplies its own issuer, audience, and credentials. The JWKS cache is local to each consumer and requires neither a shared database table nor Redis. The library never contains private keys or chooses `kid`; user-service remains responsible for signing keys and their rotation.

#### Component Interaction and Flow

1. The user authenticates through the existing mechanism.
2. Portfolio validates selection, ownership, and active state and applies early returns.
3. The local provider obtains a current token or requests another from `/oauth2/token`.
4. User-service authenticates the client, verifies grant and scopes, and signs a machine token.
5. `MarketDataClient` presents that token on the price request.
6. MDS validates signature, issuer, timestamps, required expiration, audience, and machine identity, then enforces scope and caller policy.
7. Existing price and composition flows continue without business-rule changes.

Initial proposed contract:

| Element | Rule |
|---|---|
| Initial client | `portfolio-service` |
| Grant | Only `client_credentials` |
| Authentication | `client_secret_basic`, over TLS |
| Audience | `market-data-service` |
| Scope | `market-prices:read` |
| Identity | Explicit issuer-controlled claim for machine clients |
| Signature | Asymmetric, separate from user tokens |
| Lifetime | 15 minutes by default, configurable |
| Refresh token | None; request another access token |
| Public keys | JWKS with `kid` |

Exact machine-claim and issuer values must be fixed before consumer implementation. MDS rejects user tokens even when valid for Portfolio. Invalid token: 401; valid machine token lacking permissions: 403. Do not assume Client Credentials automatically creates a `grant_type` claim.

Issued JWTs are not immediately invalidated by changing the client secret or disabling the client: they normally remain valid until expiration. Per-request revocation in MDS is not included.

#### Implementation Outline

Each story has a bounded implementation scope and includes its tests and documentation. Titles are proposals, not created tickets.

| ID | Story | Scope | SP | Main acceptance criteria |
|---|---|---|---:|---|
| US-1 | Client Credentials issuance and isolation | user-service | 5 | OAuth chain, claims contract, invalid credentials/scopes/grants rejected, no refresh, user and Prometheus regressions. Test clients and keys; not yet production-enabled. |
| US-2 | Configuration-backed clients | user-service | 5 | Clients, scopes, audiences, and TTL loaded into an in-memory repository; secrets supplied externally and encoded; invalid configuration rejected; restart rebuilds registrations from configuration. No OAuth client schema or database provisioning. |
| US-3 | Durable keys, JWKS, and rotation | user-service | 3 | External keys, stable issuer, public-only output, `kid` transition, clear failure for missing critical configuration. |
| AUTH-LIB-1 | Shared OAuth2 client and Resource Server capabilities | shared library | To estimate | Independent components for token acquisition/reuse and token/JWKS validation; consumer-specific configuration; tests for both roles; no private keys, `kid`, or microservice-specific permission policy. |
| PF-1 | Token acquisition and reuse for MDS | portfolio-service | 5 | OAuth2 Client, stable identity, renewal, remove user-token forwarding, dependency failure without fallback; invalid selection acquires no token. |
| MDS-1 | Service-only internal reads | market-data-service | 5 | Resource Server, complete contract and scope, user rejection, obsolete-auth cleanup, preserved errors and Prometheus. |
| OPS-1 | Integrated deployment | Infrastructure | 3 | Compose configuration, URLs, secrets and TLS, network restrictions, coordinated activation and end-to-end verification; no service-code changes. |
| US-4 | Token-endpoint abuse protection | user-service | 3 | Limit abuse before token processing, tests and safe logs; required before shared exposure unless infrastructure provides equivalent protection. |

Baseline for the listed microservice stories: **26 SP**, plus AUTH-LIB-1, still to be estimated. Additional endpoint protection: **3 SP**, totaling **29 SP + AUTH-LIB-1** if implemented in user-service. These relative estimates require team calibration. The initial 8–13 SP estimate for the whole integration is revised: 13 SP fits the issuer block, not the complete scope.

Order: US-1 → US-2 → US-3 → AUTH-LIB-1 → PF-1 and MDS-1 (these two can run in parallel using the library); OPS-1 validates integration. US-4 or equivalent protection must precede endpoint exposure.

Minimum verification:

- Production chains and real servlet-server execution for filter isolation; no login, refresh, logout, revocation, or Prometheus regressions.
- Configuration-loaded clients, grant/scope/audience restrictions, no plaintext client secrets in registrations, and restart behavior.
- In Portfolio, reuse and expiration, machine tokens on both reads, no acquisition for invalid selections, issuer failure without fallback or user-login errors.
- In MDS, signed test JWTs/JWKS: invalid signature, issuer, audience, or expiration; missing expiration; human identity even under the same issuer; insufficient permissions; Basic does not authorize prices and machine Bearer does not replace Prometheus.
- Issuer restart/outage, key availability, expiration, error behavior, and coordinated activation across the complete flow.

## Open Questions

1. **Issuer and environment URLs:** fix a stable `iss` and token/JWKS access from host and containers. Resolve through topology and configuration, not by deriving identity from each request hostname.
2. **Machine claim and client policy:** specify claim name/value and client-identity relationship, plus audience assignment as destinations grow. Requires an issuer/recipient contract and negative tests before PF-1 and MDS-1.
3. **Authorization persistence:** client registrations are configuration-backed and in memory. Decide separately whether authorization records are needed for introspection or revocation; persistence does not make MDS revoke locally validated JWTs. Avoid unnecessary consent tables.
4. **Provisioning and secrets:** define environment-specific client values and secure secret delivery/rotation; keep production credentials out of Git.
5. **OAuth protection:** confirm user-service or infrastructure rate limiting, with effective coverage before shared exposure.
6. **Operational parameters:** confirm clock skew, timeouts, and limits before deployment; the default token lifetime is 15 minutes and remains configurable.

# User Service Overview

The `user-service` is an implemented Spring Boot backend service providing authentication functionality for registration, login, and token management.

## Technical Overview

### Service Details

The `user-service` Spring Boot backend provides authentication functionality for user registration, login, and token management.

**Service Information:**
- **Service Name:** `user-service`
- **Default Port:** `8080`
- **Components:**
    - HTTP Endpoints: `6`
    - Persisted Entities: `4`
    - Implementation Slices: `9`

### Responsibilities

This service implements the authentication API contract, orchestrating user registration, login, and token refresh operations. Key functions include:

- Managing user credentials and security-related data using JPA entities
- Orchestrating validation, token generation, security logic, and persistence updates
- Handling authentication state management

### System Architecture

The service comprises these core layers:

- **API:** Exposes functionality through controllers
- **Services & Helpers:** Implements authentication workflows, validation, token handling, and domain logic
- **Data Access Layer:** Manages persistence via JPA entities and repositories
- **Configuration:** Implements security settings, interceptors, and dependency injection
- **Mappers:** Converts domain objects between layers
- **Runtime Components:** Manages application lifecycle and context

## Security & Operations

### Authentication Security

**Password Handling:** Credential storage uses external BCrypt hashing.

#### Access Tokens
- Signed with HS256 algorithm
- Signing key defined as `app.auth.jwt.secret`
- **Token TTL:** `${JWT_ACCESS_TOKEN_TTL_SECONDS:900}` seconds

#### Refresh Tokens
- Generated using SecureRandom
- Stored in a dedicated `refresh_tokens` table
- **Token TTL:** `${JWT_REFRESH_TOKEN_TTL_SECONDS:1209600}` seconds

### Operational Protections

**Rate Limiting:**
- Registration: `5` requests per `300 sec` per IP
- Login: `10` requests per `60 sec` per IP
- Refresh: `10` requests per `60 sec` per IP

**Observability:**
- Authentication request cycle tracking
- API security interactions monitoring
- Rate limiting event logging

### Configuration Security
Sensitive settings use environment variable substitution:
- `app.auth.jwt.secret`
- `JWT_ACCESS_TOKEN_TTL_SECONDS`
- `JWT_REFRESH_TOKEN_TTL_SECONDS`

## Related Documentation

For further details, reference these components:
- `docs/knowledge/services/user-service/api.md`
- `docs/knowledge/services/user-service/data-model.md`
- `docs/knowledge/services/user-service/runtime.md`
- `docs/knowledge/services/user-service/observability.md`
- `docs/knowledge/project-context.md`

# Service Auth Support

Shared service-to-service authentication support for Tyche Wealth. It centralizes the OAuth2 Client
Credentials flow and common JWT validation rules while leaving each microservice responsible for its
own client configuration, routes, and authorization policy.

## Features

### OAuth2 Client

When `spring-boot-starter-oauth2-client` is on the classpath, the library configures an
`OAuth2AuthorizedClientManager` for Client Credentials and exposes `ServiceAccessTokenProvider`.
Spring handles token reuse and obtains another token when the current one expires.

Configure the client registration with standard Spring Boot properties:

```properties
spring.security.oauth2.client.registration.market-data.provider=internal
spring.security.oauth2.client.registration.market-data.client-id=portfolio-service
spring.security.oauth2.client.registration.market-data.client-secret=${MDS_CLIENT_SECRET}
spring.security.oauth2.client.registration.market-data.authorization-grant-type=client_credentials
spring.security.oauth2.client.registration.market-data.scope=market-prices:read
spring.security.oauth2.client.provider.internal.token-uri=${TOKEN_URI}
```

Obtain a token using the registration ID and a stable technical identity:

```java
String token = tokenProvider.getAccessToken("market-data", "portfolio-service");
```

### Resource Server

When `spring-boot-starter-oauth2-resource-server` is on the classpath, enable the configuration and
provide the trusted issuer, JWKS URL, and expected audience:

```properties
app.service-auth.resource-server.enabled=true
app.service-auth.resource-server.issuer=${SERVICE_TOKEN_ISSUER}
app.service-auth.resource-server.jwk-set-uri=${SERVICE_TOKEN_JWKS_URI}
app.service-auth.resource-server.audience=market-data-service
```

The library configures a `NimbusJwtDecoder` that validates the JWKS signature, issuer, audience, and
the presence and validity of `exp`. Nimbus manages the JWKS cache.

The service must enable JWT support in its own `SecurityFilterChain` and define its authorized routes
and scopes. This library does not issue tokens, contain private keys, or define service-specific
authorization policies.

The OAuth2 Client and Resource Server starters are optional: add only the starter required by each
consumer. Jackson is included to process JSON responses from the OAuth token endpoint.

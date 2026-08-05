package com.tychewealth.dto.auth;

/**
 * Internal representation of a generated authentication token.
 *
 * <p>Stores the token type, serialized token value, lifetime, and JWT identifier used by the
 * authentication workflows for delivery, persistence, or token-state management.
 */
public record AuthTokenDto(String tokenType, String token, long expiresIn, String jti) {}

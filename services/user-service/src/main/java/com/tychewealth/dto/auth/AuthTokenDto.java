package com.tychewealth.dto.auth;

public record AuthTokenDto(String tokenType, String token, long expiresIn, String jti) {}

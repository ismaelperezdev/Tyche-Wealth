package com.tychewealth.dto.auth;

public record AuthTokenDto(String tokenType, String accessToken, long expiresIn, String jti) {}

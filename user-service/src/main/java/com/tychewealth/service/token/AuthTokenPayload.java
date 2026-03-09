package com.tychewealth.service.token;

public record AuthTokenPayload(String tokenType, String accessToken, long expiresIn) {}

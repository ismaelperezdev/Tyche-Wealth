package com.tychewealth.dto.auth;

import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.service.token.AuthTokenPayload;

public record RegisteredUserResultDto(
    UserResponseDto response, AuthTokenPayload verificationToken) {}

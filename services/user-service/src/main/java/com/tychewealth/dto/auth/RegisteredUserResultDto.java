package com.tychewealth.dto.auth;

import com.tychewealth.dto.user.UserResponseDto;

public record RegisteredUserResultDto(UserResponseDto response, AuthTokenDto verificationToken) {}

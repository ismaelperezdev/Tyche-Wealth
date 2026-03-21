package com.tychewealth.dto.auth;

import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.service.token.AuthTokenPayload;

public record RegisteredUserResultDto(
    UserEntity user, UserResponseDto response, AuthTokenPayload verificationToken) {}

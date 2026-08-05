package com.tychewealth.dto.auth;

import com.tychewealth.dto.user.UserResponseDto;

/**
 * Internal result produced after registering a user.
 *
 * <p>Combines the public user response with the verification token required by the email workflow
 * so the service can persist the account result and schedule verification delivery together.
 */
public record RegisteredUserResultDto(UserResponseDto response, AuthTokenDto verificationToken) {}

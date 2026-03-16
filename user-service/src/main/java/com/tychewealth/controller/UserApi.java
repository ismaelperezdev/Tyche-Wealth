package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.REQUEST_CONSUMES;
import static com.tychewealth.constants.ApiConstants.REQUEST_PRODUCES;
import static com.tychewealth.constants.ApiConstants.USER_BASE_URL;

import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.dto.user.request.UserPasswordUpdateRequestDto;
import com.tychewealth.dto.user.request.UserUpdateRequestDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(value = USER_BASE_URL)
@Tag(name = "User")
public interface UserApi {

  @GetMapping(value = "/me")
  ResponseEntity<UserResponseDto> retrieve(@AuthenticationPrincipal Long userId);

  @PatchMapping(value = "/me", consumes = REQUEST_CONSUMES, produces = REQUEST_PRODUCES)
  ResponseEntity<UserResponseDto> update(
      @AuthenticationPrincipal Long userId, @Valid @RequestBody UserUpdateRequestDto updateRequest);

  @PatchMapping(value = "/me/password", consumes = REQUEST_CONSUMES)
  ResponseEntity<Void> updatePassword(
      @AuthenticationPrincipal Long userId,
      @Valid @RequestBody UserPasswordUpdateRequestDto updatePasswordRequest);

  @DeleteMapping(value = "/me")
  ResponseEntity<Void> delete(@AuthenticationPrincipal Long userId);
}

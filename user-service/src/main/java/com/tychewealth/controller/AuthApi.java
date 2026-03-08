package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.*;

import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.dto.user.request.UserCreateRequestDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(value = URL_FOLDER_V1 + "/auth")
@Tag(name = "Auth")
public interface AuthApi {

  @PostMapping(value = "/register", consumes = REQUEST_CONSUMES, produces = REQUEST_PRODUCES)
  ResponseEntity<UserResponseDto> register(@Valid @RequestBody UserCreateRequestDto register);
}

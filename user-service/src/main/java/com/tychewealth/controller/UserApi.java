package com.tychewealth.controller;

import static com.tychewealth.constants.ApiConstants.URL_FOLDER_V1;

import com.tychewealth.dto.user.UserResponseDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(value = URL_FOLDER_V1 + "/user")
@Tag(name = "User")
public interface UserApi {

  @GetMapping(value = "/me")
  ResponseEntity<UserResponseDto> retrieve(
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader);

  @DeleteMapping(value = "/me")
  ResponseEntity<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader);
}

package com.tychewealth.controller.impl;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.controller.UserApi;
import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.dto.user.request.UserPasswordUpdateRequestDto;
import com.tychewealth.dto.user.request.UserUpdateRequestDto;
import com.tychewealth.service.UserService;
import com.tychewealth.utils.LogContextFactory;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@AllArgsConstructor
public class UserApiController implements UserApi {

  private final UserService userService;

  @Override
  public ResponseEntity<UserResponseDto> retrieve(
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
    log.info(LogConstants.REQUEST_START, LogConstants.USER, LogConstants.RETRIEVE_ACTION);

    UserResponseDto response = userService.retrieve(authorizationHeader);

    log.info(
        LogConstants.REQUEST_SUCCESS + LogConstants.USER_ID,
        LogConstants.USER,
        LogConstants.RETRIEVE_ACTION,
        response.getId());

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @Override
  public ResponseEntity<UserResponseDto> update(
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
      @Valid @RequestBody UserUpdateRequestDto updateRequest) {
    log.info(
        LogConstants.REQUEST_START + LogConstants.UPDATE_REQUEST_FIELDS,
        LogConstants.USER,
        LogConstants.UPDATE_ACTION,
        LogContextFactory.mask(updateRequest.getUsername()));

    UserResponseDto response = userService.update(authorizationHeader, updateRequest);

    log.info(
        LogConstants.REQUEST_SUCCESS + LogConstants.USER_ID,
        LogConstants.USER,
        LogConstants.UPDATE_ACTION,
        response.getId());

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @Override
  public ResponseEntity<Void> updatePassword(
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
      @Valid @RequestBody UserPasswordUpdateRequestDto updatePasswordRequest) {
    log.info(LogConstants.REQUEST_START, LogConstants.USER, LogConstants.UPDATE_PASSWORD_ACTION);

    Long updatedUserId = userService.updatePassword(authorizationHeader, updatePasswordRequest);

    log.info(
        LogConstants.REQUEST_SUCCESS + LogConstants.USER_ID,
        LogConstants.USER,
        LogConstants.UPDATE_PASSWORD_ACTION,
        updatedUserId);

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
    log.info(LogConstants.REQUEST_START, LogConstants.USER, LogConstants.DELETE_ACTION);

    Long deletedUserId = userService.delete(authorizationHeader);

    log.info(
        LogConstants.REQUEST_SUCCESS + LogConstants.USER_ID,
        LogConstants.USER,
        LogConstants.DELETE_ACTION,
        deletedUserId);

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}

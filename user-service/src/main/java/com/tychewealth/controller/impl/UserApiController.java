package com.tychewealth.controller.impl;

import static com.tychewealth.constants.AuthConstants.AUTHORIZATION_HEADER;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.controller.UserApi;
import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.dto.user.request.UserPasswordUpdateRequestDto;
import com.tychewealth.dto.user.request.UserUpdateRequestDto;
import com.tychewealth.service.UserService;
import com.tychewealth.service.monitoring.UserMetrics;
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
  private final UserMetrics userMetrics;

  @Override
  public ResponseEntity<UserResponseDto> retrieve(
      @RequestHeader(value = AUTHORIZATION_HEADER, required = false) String authorizationHeader) {
    userMetrics.recordRetrieveRequest();
    log.info(LogConstants.REQUEST_START, LogConstants.USER, LogConstants.RETRIEVE_ACTION);

    UserResponseDto response = userService.retrieve(authorizationHeader);
    userMetrics.recordRetrieveSuccess();

    log.info(
        LogConstants.REQUEST_SUCCESS + LogConstants.USER_ID,
        LogConstants.USER,
        LogConstants.RETRIEVE_ACTION,
        response.getId());

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @Override
  public ResponseEntity<UserResponseDto> update(
      @RequestHeader(value = AUTHORIZATION_HEADER, required = false) String authorizationHeader,
      @Valid @RequestBody UserUpdateRequestDto updateRequest) {
    userMetrics.recordUpdateRequest();
    log.info(
        LogConstants.REQUEST_START + LogConstants.UPDATE_REQUEST_FIELDS,
        LogConstants.USER,
        LogConstants.UPDATE_ACTION,
        LogContextFactory.mask(updateRequest.getUsername()));

    UserResponseDto response = userService.update(authorizationHeader, updateRequest);
    userMetrics.recordUpdateSuccess();

    log.info(
        LogConstants.REQUEST_SUCCESS + LogConstants.USER_ID,
        LogConstants.USER,
        LogConstants.UPDATE_ACTION,
        response.getId());

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @Override
  public ResponseEntity<Void> updatePassword(
      @RequestHeader(value = AUTHORIZATION_HEADER, required = false) String authorizationHeader,
      @Valid @RequestBody UserPasswordUpdateRequestDto updatePasswordRequest) {
    userMetrics.recordUpdatePasswordRequest();
    log.info(LogConstants.REQUEST_START, LogConstants.USER, LogConstants.UPDATE_PASSWORD_ACTION);

    Long updatedUserId = userService.updatePassword(authorizationHeader, updatePasswordRequest);
    userMetrics.recordUpdatePasswordSuccess();

    log.info(
        LogConstants.REQUEST_SUCCESS + LogConstants.USER_ID,
        LogConstants.USER,
        LogConstants.UPDATE_PASSWORD_ACTION,
        updatedUserId);

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<Void> delete(
      @RequestHeader(value = AUTHORIZATION_HEADER, required = false) String authorizationHeader) {
    userMetrics.recordDeleteRequest();
    log.info(LogConstants.REQUEST_START, LogConstants.USER, LogConstants.DELETE_ACTION);

    Long deletedUserId = userService.delete(authorizationHeader);
    userMetrics.recordDeleteSuccess();

    log.info(
        LogConstants.REQUEST_SUCCESS + LogConstants.USER_ID,
        LogConstants.USER,
        LogConstants.DELETE_ACTION,
        deletedUserId);

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}

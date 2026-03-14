package com.tychewealth.controller.impl;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.controller.UserApi;
import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        LogConstants.REQUEST_SUCCESS + LogConstants.RETRIEVE_USER_ID,
        LogConstants.USER,
        LogConstants.RETRIEVE_ACTION,
        response.getId());
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @Override
  public ResponseEntity<Void> delete(
      @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
    log.info(LogConstants.REQUEST_START, LogConstants.USER, LogConstants.DELETE_ACTION);

    Long deletedUserId = userService.delete(authorizationHeader);
    log.info(
        LogConstants.REQUEST_SUCCESS + LogConstants.DELETE_USER_ID,
        LogConstants.USER,
        LogConstants.DELETE_ACTION,
        deletedUserId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}

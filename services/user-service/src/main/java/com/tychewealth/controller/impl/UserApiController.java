package com.tychewealth.controller.impl;

import static com.tychewealth.constants.AuthConstants.AUTHORIZATION_HEADER;
import static com.tychewealth.constants.SecurityConstants.CACHE_CONTROL_NO_STORE_HEADER_VALUE;
import static com.tychewealth.constants.SecurityConstants.PRAGMA_NO_CACHE_HEADER_VALUE;

import com.tychewealth.constants.LogConstants;
import com.tychewealth.controller.UserApi;
import com.tychewealth.dto.user.UserResponseDto;
import com.tychewealth.dto.user.request.UserPasswordUpdateRequestDto;
import com.tychewealth.dto.user.request.UserUpdateRequestDto;
import com.tychewealth.enums.UserMetricEnum;
import com.tychewealth.monitoring.UserMetrics;
import com.tychewealth.service.UserService;
import com.tychewealth.utils.LogContextFactory;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST adapter for the authenticated user-account endpoints declared by {@link UserApi}.
 *
 * <p>Delegates account operations to {@link UserService}, records request and success metrics, and
 * produces the corresponding HTTP responses. Sensitive operations such as password changes and
 * account deletion preserve no-store response headers and pass the authorization header to the
 * service layer for token invalidation.
 */
@Slf4j
@RestController
@AllArgsConstructor
public class UserApiController implements UserApi {

  private final UserService userService;
  private final UserMetrics userMetrics;

  @Override
  public ResponseEntity<UserResponseDto> retrieve(@AuthenticationPrincipal Long userId) {
    userMetrics.incrementMetric(UserMetricEnum.RETRIEVE_REQUESTS);
    log.info(LogConstants.REQUEST_START, LogConstants.USER, LogConstants.RETRIEVE_ACTION);

    UserResponseDto response = userService.retrieve(userId);
    userMetrics.incrementMetric(UserMetricEnum.RETRIEVE_SUCCESS);

    log.info(
        LogConstants.REQUEST_SUCCESS + LogConstants.USER_ID,
        LogConstants.USER,
        LogConstants.RETRIEVE_ACTION,
        response.getId());

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @Override
  public ResponseEntity<UserResponseDto> update(
      @AuthenticationPrincipal Long userId,
      @Valid @RequestBody UserUpdateRequestDto updateRequest) {
    userMetrics.incrementMetric(UserMetricEnum.UPDATE_REQUESTS);
    log.info(
        LogConstants.REQUEST_START + LogConstants.UPDATE_REQUEST_FIELDS,
        LogConstants.USER,
        LogConstants.UPDATE_ACTION,
        LogContextFactory.mask(updateRequest.getUsername()));

    UserResponseDto response = userService.update(userId, updateRequest);
    userMetrics.incrementMetric(UserMetricEnum.UPDATE_SUCCESS);

    log.info(
        LogConstants.REQUEST_SUCCESS + LogConstants.USER_ID,
        LogConstants.USER,
        LogConstants.UPDATE_ACTION,
        response.getId());

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @Override
  public ResponseEntity<Void> updatePassword(
      @AuthenticationPrincipal Long userId,
      @RequestHeader(value = AUTHORIZATION_HEADER, required = false) String authorizationHeader,
      @Valid @RequestBody UserPasswordUpdateRequestDto updatePasswordRequest) {
    userMetrics.incrementMetric(UserMetricEnum.UPDATE_PASSWORD_REQUESTS);
    log.info(LogConstants.REQUEST_START, LogConstants.USER, LogConstants.UPDATE_PASSWORD_ACTION);

    Long updatedUserId =
        userService.updatePassword(userId, authorizationHeader, updatePasswordRequest);
    userMetrics.incrementMetric(UserMetricEnum.UPDATE_PASSWORD_SUCCESS);

    log.info(
        LogConstants.REQUEST_SUCCESS + LogConstants.USER_ID,
        LogConstants.USER,
        LogConstants.UPDATE_PASSWORD_ACTION,
        updatedUserId);

    return ResponseEntity.status(HttpStatus.NO_CONTENT)
        .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_NO_STORE_HEADER_VALUE)
        .header(HttpHeaders.PRAGMA, PRAGMA_NO_CACHE_HEADER_VALUE)
        .build();
  }

  @Override
  public ResponseEntity<Void> delete(
      @AuthenticationPrincipal Long userId,
      @RequestHeader(value = AUTHORIZATION_HEADER, required = false) String authorizationHeader) {
    userMetrics.incrementMetric(UserMetricEnum.DELETE_REQUESTS);
    log.info(LogConstants.REQUEST_START, LogConstants.USER, LogConstants.DELETE_ACTION);

    Long deletedUserId = userService.delete(userId, authorizationHeader);
    userMetrics.incrementMetric(UserMetricEnum.DELETE_SUCCESS);

    log.info(
        LogConstants.REQUEST_SUCCESS + LogConstants.USER_ID,
        LogConstants.USER,
        LogConstants.DELETE_ACTION,
        deletedUserId);

    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}

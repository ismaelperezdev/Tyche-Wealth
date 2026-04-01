package com.tychewealth.trusteddevice;

import static com.tychewealth.constants.TestConstants.TEST_TRUSTED_DEVICE_COOKIE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.entity.TrustedDeviceEntity;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.error.exception.AuthException;
import com.tychewealth.error.handler.ErrorDefinition;
import com.tychewealth.repository.TrustedDeviceRepository;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.trusteddevice.TrustedDeviceManager;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;

@ExtendWith(MockitoExtension.class)
class TrustedDeviceManagerTest {

  private static final Long TRUSTED_DEVICE_USER_ID = 7L;
  private static final Long TRUSTED_DEVICE_LIMIT_USER_ID = 9L;

  @Mock private TrustedDeviceRepository trustedDeviceRepository;
  @Mock private UserRepository userRepository;

  private TrustedDeviceManager trustedDeviceManager;

  @BeforeEach
  void setUp() {
    trustedDeviceManager = new TrustedDeviceManager(trustedDeviceRepository, userRepository, true);
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void createTrustedDeviceCookieLocksUserBeforeCheckingQuotaAndSaving() {
    UserEntity user = new UserEntity();
    user.setId(TRUSTED_DEVICE_USER_ID);

    when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
    when(trustedDeviceRepository.countByUserIdAndExpiresAtAfter(anyLong(), any(Instant.class)))
        .thenReturn(0L);

    var cookie = trustedDeviceManager.createTrustedDeviceCookie(user);

    ArgumentCaptor<TrustedDeviceEntity> trustedDeviceCaptor =
        ArgumentCaptor.forClass(TrustedDeviceEntity.class);
    InOrder inOrder = inOrder(userRepository, trustedDeviceRepository);
    inOrder.verify(userRepository).findByIdForUpdate(user.getId());
    inOrder
        .verify(trustedDeviceRepository)
        .deleteByUserIdAndExpiresAtBefore(anyLong(), any(Instant.class));
    inOrder
        .verify(trustedDeviceRepository)
        .countByUserIdAndExpiresAtAfter(anyLong(), any(Instant.class));
    inOrder.verify(trustedDeviceRepository).save(trustedDeviceCaptor.capture());

    TrustedDeviceEntity savedTrustedDevice = trustedDeviceCaptor.getValue();
    assertEquals(user, savedTrustedDevice.getUser());
    assertNotNull(savedTrustedDevice.getTokenHash());
    assertNotNull(savedTrustedDevice.getExpiresAt());
    assertNotNull(savedTrustedDevice.getLastUsedAt());
    assertEquals(TEST_TRUSTED_DEVICE_COOKIE_NAME, cookie.getName());
  }

  @Test
  void createTrustedDeviceCookieThrowsConflictWhenQuotaIsReachedAfterLock() {
    UserEntity user = new UserEntity();
    user.setId(TRUSTED_DEVICE_LIMIT_USER_ID);

    when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
    when(trustedDeviceRepository.countByUserIdAndExpiresAtAfter(anyLong(), any(Instant.class)))
        .thenReturn(3L);

    AuthException exception =
        assertThrows(
            AuthException.class, () -> trustedDeviceManager.createTrustedDeviceCookie(user));

    assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus());
    assertEquals(ErrorDefinition.AUTH_TRUSTED_DEVICE_LIMIT_REACHED, exception.getErrorDefinition());
    verify(trustedDeviceRepository, never()).save(any(TrustedDeviceEntity.class));
  }
}

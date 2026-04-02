package com.tychewealth.service.trusteddevice;

import static com.tychewealth.constants.TestConstants.TEST_EMAIL_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_TRUSTED_DEVICE_COOKIE_NAME;
import static com.tychewealth.constants.TestConstants.TEST_TRUSTED_DEVICE_TOKEN;
import static com.tychewealth.testdata.EntityBuilder.buildUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.tychewealth.utils.Utils;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class TrustedDeviceManagerTest {

  private static final Long TRUSTED_DEVICE_USER_ID = 7L;
  private static final Long TRUSTED_DEVICE_LIMIT_USER_ID = 9L;
  private static final Duration TEST_COOKIE_MAX_AGE = Duration.ofHours(1);

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
    UserEntity user = buildUser(TEST_EMAIL_LAURA, null, null);
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
    UserEntity user = buildUser(TEST_EMAIL_LAURA, null, null);
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

  @Test
  void createTrustedDeviceCookieReusesExistingTrustedDeviceWhenCookieMatches() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, null, null);
    user.setId(TRUSTED_DEVICE_USER_ID);
    Instant now = Instant.now();
    TrustedDeviceEntity trustedDevice = new TrustedDeviceEntity();
    trustedDevice.setUser(user);
    trustedDevice.setTokenHash(Utils.sha256Hex(TEST_TRUSTED_DEVICE_TOKEN));
    trustedDevice.setExpiresAt(now.plus(Duration.ofDays(30)));
    trustedDevice.setLastUsedAt(now.minus(Duration.ofHours(1)));
    setTrustedDeviceCookie();

    when(trustedDeviceRepository.findByUserIdAndTokenHashAndExpiresAtAfter(
            anyLong(), anyString(), any(Instant.class)))
        .thenReturn(Optional.of(trustedDevice));

    var cookie = trustedDeviceManager.createTrustedDeviceCookie(user);

    assertEquals(TEST_TRUSTED_DEVICE_COOKIE_NAME, cookie.getName());
    assertEquals(TEST_TRUSTED_DEVICE_TOKEN, cookie.getValue());
    verify(trustedDeviceRepository).save(trustedDevice);
    verify(userRepository, never()).findByIdForUpdate(anyLong());
    verify(trustedDeviceRepository, never()).countByUserIdAndExpiresAtAfter(anyLong(), any());
  }

  @Test
  void createTrustedDeviceCookieThrowsUnauthorizedWhenUserCannotBeLocked() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, null, null);
    user.setId(TRUSTED_DEVICE_USER_ID);

    when(userRepository.findByIdForUpdate(TRUSTED_DEVICE_USER_ID)).thenReturn(Optional.empty());

    AuthException exception =
        assertThrows(
            AuthException.class, () -> trustedDeviceManager.createTrustedDeviceCookie(user));

    assertEquals(HttpStatus.UNAUTHORIZED, exception.getHttpStatus());
    assertEquals(ErrorDefinition.UNAUTHORIZED, exception.getErrorDefinition());
    verify(trustedDeviceRepository, never()).save(any(TrustedDeviceEntity.class));
  }

  @Test
  void buildTrustedDeviceCookieSetsExpectedSecurityAttributes() {
    var cookie =
        trustedDeviceManager.buildTrustedDeviceCookie(
            TEST_TRUSTED_DEVICE_TOKEN, TEST_COOKIE_MAX_AGE);

    assertEquals(TEST_TRUSTED_DEVICE_COOKIE_NAME, cookie.getName());
    assertEquals(TEST_TRUSTED_DEVICE_TOKEN, cookie.getValue());
    assertTrue(cookie.isHttpOnly());
    assertTrue(cookie.isSecure());
    assertEquals("Lax", cookie.getSameSite());
    assertEquals("/", cookie.getPath());
    assertEquals(TEST_COOKIE_MAX_AGE, cookie.getMaxAge());
  }

  @Test
  void hasReachedTrustedDeviceLimitDeletesExpiredDevicesBeforeCounting() {
    when(trustedDeviceRepository.countByUserIdAndExpiresAtAfter(anyLong(), any(Instant.class)))
        .thenReturn(3L);

    boolean result =
        trustedDeviceManager.hasReachedTrustedDeviceLimit(TRUSTED_DEVICE_LIMIT_USER_ID);

    InOrder inOrder = inOrder(trustedDeviceRepository);
    assertTrue(result);
    inOrder
        .verify(trustedDeviceRepository)
        .deleteByUserIdAndExpiresAtBefore(anyLong(), any(Instant.class));
    inOrder
        .verify(trustedDeviceRepository)
        .countByUserIdAndExpiresAtAfter(anyLong(), any(Instant.class));
  }

  @Test
  void isTrustedCurrentDeviceReturnsTrueWhenCookieMatchesActiveTrustedDevice() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, null, null);
    user.setId(TRUSTED_DEVICE_USER_ID);
    setTrustedDeviceCookie();

    when(trustedDeviceRepository.findByUserIdAndTokenHashAndExpiresAtAfter(
            anyLong(), anyString(), any(Instant.class)))
        .thenReturn(Optional.of(new TrustedDeviceEntity()));

    boolean result = trustedDeviceManager.isTrustedCurrentDevice(user);

    assertTrue(result);
  }

  @Test
  void isTrustedCurrentDeviceReturnsFalseWhenNoCookieIsPresent() {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, null, null);
    user.setId(TRUSTED_DEVICE_USER_ID);

    boolean result = trustedDeviceManager.isTrustedCurrentDevice(user);

    assertFalse(result);
    verify(trustedDeviceRepository, never())
        .findByUserIdAndTokenHashAndExpiresAtAfter(anyLong(), anyString(), any(Instant.class));
  }

  private void setTrustedDeviceCookie() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie(TEST_TRUSTED_DEVICE_COOKIE_NAME, TEST_TRUSTED_DEVICE_TOKEN));
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }
}

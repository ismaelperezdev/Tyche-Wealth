package com.tychewealth.controller;

import static com.tychewealth.constants.TestConstants.TEST_EMAIL_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_PASSWORD_VALID;
import static com.tychewealth.constants.TestConstants.TEST_TRUSTED_DEVICE_COOKIE_NAME;
import static com.tychewealth.constants.TestConstants.TEST_USERNAME_LAURA;
import static com.tychewealth.testdata.EntityBuilder.buildUser;
import static com.tychewealth.testhelper.AuthTestHelper.extractCookieValue;
import static com.tychewealth.testhelper.AuthTestHelper.forgotPasswordStatus;
import static com.tychewealth.testhelper.AuthTestHelper.resendVerificationStatus;
import static com.tychewealth.testhelper.AuthTestHelper.verifyLoginDeviceRequest;
import static com.tychewealth.testhelper.AuthTestHelper.verifyLoginDeviceStatus;
import static com.tychewealth.testhelper.AuthTestHelper.verifyRegistrationRequest;
import static com.tychewealth.testhelper.AuthTestHelper.verifyRegistrationStatus;
import static com.tychewealth.testhelper.ConcurrentTestHelper.runConcurrently;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tychewealth.config.AuthIntegrationTestConfig;
import com.tychewealth.config.AuthRateLimitConfig;
import com.tychewealth.dto.auth.AuthTokenDto;
import com.tychewealth.dto.auth.request.ForgotPasswordRequestDto;
import com.tychewealth.dto.auth.request.ResendVerificationEmailRequestDto;
import com.tychewealth.dto.email.request.EmailMessageDto;
import com.tychewealth.email.EmailSender;
import com.tychewealth.entity.UserEntity;
import com.tychewealth.enums.AccessTokenType;
import com.tychewealth.enums.EmailSendResult;
import com.tychewealth.repository.RefreshTokenRepository;
import com.tychewealth.repository.TrustedDeviceRepository;
import com.tychewealth.repository.UserRepository;
import com.tychewealth.service.token.AccessTokenCodec;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AuthIntegrationTestConfig.class)
@ContextConfiguration(initializers = AuthIntegrationTestConfig.Initializer.class)
@AutoConfigureMockMvc
class AuthIdempotencyIntegrationTest {

  private static final String FORGOT_PASSWORD_KEY_PREFIX = "forgot-password:";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private TrustedDeviceRepository trustedDeviceRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private EmailSender emailSender;
  @Autowired private AuthRateLimitConfig rateLimitConfig;
  @Autowired private StringRedisTemplate stringRedisTemplate;
  @Autowired private AccessTokenCodec accessTokenCodec;

  private final Map<String, String> forgotPasswordTokens = new ConcurrentHashMap<>();

  @BeforeEach
  void setUp() {
    refreshTokenRepository.deleteAll();
    trustedDeviceRepository.deleteAll();
    userRepository.deleteAll();
    rateLimitConfig.resetAll();
    reset(emailSender);
    when(emailSender.send(any(EmailMessageDto.class))).thenReturn(EmailSendResult.DELIVERED);

    ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
    when(valueOperations.setIfAbsent(any(), any(), any(Duration.class)))
        .thenAnswer(
            invocation ->
                forgotPasswordTokens.putIfAbsent(
                        invocation.getArgument(0), invocation.getArgument(1))
                    == null);
    org.mockito.Mockito.doAnswer(
            invocation -> {
              forgotPasswordTokens.remove(invocation.getArgument(0));
              return true;
            })
        .when(stringRedisTemplate)
        .delete(any(String.class));
  }

  @Test
  void resendVerificationEmailConcurrentRetriesSendOnlyOneEmailAndKeepConsistentState()
      throws Exception {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, null);
    user.setPassword(passwordEncoder.encode(TEST_PASSWORD_VALID));
    user.setVerified(false);
    user.setVerificationTokenExpiresAt(Instant.now().minusSeconds(5));
    UserEntity savedUser = userRepository.save(user);
    ResendVerificationEmailRequestDto requestDto =
        new ResendVerificationEmailRequestDto(TEST_EMAIL_LAURA);

    List<Integer> statuses =
        runConcurrently(
            () -> resendVerificationStatus(mockMvc, objectMapper, requestDto),
            () -> resendVerificationStatus(mockMvc, objectMapper, requestDto));

    assertEquals(List.of(204, 204), statuses);
    verify(emailSender, org.mockito.Mockito.times(1)).send(any(EmailMessageDto.class));

    UserEntity updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
    assertNotNull(updatedUser.getVerificationTokenExpiresAt());
    assertTrue(updatedUser.getVerificationTokenExpiresAt().isAfter(Instant.now()));
  }

  @Test
  void forgotPasswordConcurrentRetriesSendOnlyOneEmail() throws Exception {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, null);
    user.setPassword(passwordEncoder.encode(TEST_PASSWORD_VALID));
    user.setVerified(true);
    UserEntity savedUser = userRepository.save(user);
    ForgotPasswordRequestDto requestDto = new ForgotPasswordRequestDto(TEST_EMAIL_LAURA);

    List<Integer> statuses =
        runConcurrently(
            () -> forgotPasswordStatus(mockMvc, objectMapper, requestDto),
            () -> forgotPasswordStatus(mockMvc, objectMapper, requestDto));

    assertEquals(List.of(204, 204), statuses);
    verify(emailSender, org.mockito.Mockito.times(1)).send(any(EmailMessageDto.class));
    assertNotNull(forgotPasswordTokens.get(FORGOT_PASSWORD_KEY_PREFIX + savedUser.getId()));
  }

  @Test
  void verifyRegistrationConcurrentRetriesReuseExistingTrustedDevice() throws Exception {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, null);
    user.setPassword(passwordEncoder.encode(TEST_PASSWORD_VALID));
    user.setVerified(false);
    UserEntity savedUser = userRepository.save(user);
    AuthTokenDto verificationToken =
        accessTokenCodec.generateToken(savedUser, AccessTokenType.VERIFY_EMAIL);

    String setCookieHeader =
        verifyRegistrationRequest(mockMvc, verificationToken.token())
            .andReturn()
            .getResponse()
            .getHeader(HttpHeaders.SET_COOKIE);
    Cookie trustedDeviceCookie =
        new Cookie(
            TEST_TRUSTED_DEVICE_COOKIE_NAME,
            extractCookieValue(setCookieHeader, TEST_TRUSTED_DEVICE_COOKIE_NAME));

    List<Integer> statuses =
        runConcurrently(
            () -> verifyRegistrationStatus(mockMvc, verificationToken.token(), trustedDeviceCookie),
            () ->
                verifyRegistrationStatus(mockMvc, verificationToken.token(), trustedDeviceCookie));

    assertEquals(List.of(204, 204), statuses);
    assertEquals(1L, trustedDeviceRepository.count());
    assertTrue(userRepository.findById(savedUser.getId()).orElseThrow().isVerified());
  }

  @Test
  void verifyLoginDeviceConcurrentRetriesReuseExistingTrustedDevice() throws Exception {
    UserEntity user = buildUser(TEST_EMAIL_LAURA, TEST_USERNAME_LAURA, null);
    user.setPassword(passwordEncoder.encode(TEST_PASSWORD_VALID));
    user.setVerified(true);
    UserEntity savedUser = userRepository.save(user);
    AuthTokenDto verificationToken =
        accessTokenCodec.generateToken(savedUser, AccessTokenType.VERIFY_LOGIN_DEVICE);

    String setCookieHeader =
        verifyLoginDeviceRequest(mockMvc, verificationToken.token())
            .andReturn()
            .getResponse()
            .getHeader(HttpHeaders.SET_COOKIE);
    Cookie trustedDeviceCookie =
        new Cookie(
            TEST_TRUSTED_DEVICE_COOKIE_NAME,
            extractCookieValue(setCookieHeader, TEST_TRUSTED_DEVICE_COOKIE_NAME));
    rateLimitConfig.resetAll();

    List<Integer> statuses =
        runConcurrently(
            () -> verifyLoginDeviceStatus(mockMvc, verificationToken.token(), trustedDeviceCookie),
            () -> verifyLoginDeviceStatus(mockMvc, verificationToken.token(), trustedDeviceCookie));

    assertEquals(List.of(204, 204), statuses);
    assertEquals(1L, trustedDeviceRepository.count());
  }
}

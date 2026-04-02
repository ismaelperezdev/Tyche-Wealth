package com.tychewealth.service.email;

import static com.tychewealth.constants.EmailConstants.FORGOT_PASSWORD_TEMPLATE_NAME;
import static com.tychewealth.constants.EmailConstants.VERIFY_EMAIL_TEMPLATE_NAME;
import static com.tychewealth.constants.EmailConstants.VERIFY_LOGIN_DEVICE_TEMPLATE_NAME;
import static com.tychewealth.constants.TestConstants.TEST_EMAIL_LAURA;
import static com.tychewealth.constants.TestConstants.TEST_FORGOT_PASSWORD_TOKEN_TTL_SECONDS;
import static com.tychewealth.constants.TestConstants.TEST_VERIFY_EMAIL_TOKEN_TTL_SECONDS;
import static com.tychewealth.constants.TestConstants.TEST_VERIFY_LOGIN_DEVICE_TOKEN_TTL_SECONDS;
import static com.tychewealth.constants.TestConstants.TEST_VERIFY_REGISTRATION_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tychewealth.dto.email.request.EmailMessageDto;
import com.tychewealth.service.email.support.EmailTemplateSupport;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;

@ExtendWith(MockitoExtension.class)
class AuthEmailFactoryTest {

  private static final String TEST_AUTH_BASE_URI =
      "http://localhost:8080/tyche-wealth/user-service/v1/auth";
  private static final String TEST_VERIFY_LOGIN_DEVICE_PATH = "/verify-login-device";
  private static final String TEST_FORGOT_PASSWORD_URL = "http://localhost:3000/reset-password";
  private static final String TEST_VERIFY_EMAIL_TEMPLATE = "<html>verify-email-template</html>";
  private static final String TEST_VERIFY_LOGIN_TEMPLATE = "<html>verify-login-template</html>";
  private static final String TEST_FORGOT_PASSWORD_TEMPLATE =
      "<html>forgot-password-template</html>";
  private static final String TEST_VERIFY_EMAIL_TOKEN = "verify-email-token";
  private static final String TEST_VERIFY_LOGIN_DEVICE_TOKEN = "verify-login-device-token";
  private static final String TEST_FORGOT_PASSWORD_TOKEN = "forgot-password-token";
  private static final String TEST_VERIFY_EMAIL_LINK =
      TEST_AUTH_BASE_URI + TEST_VERIFY_REGISTRATION_PATH + "?token=" + TEST_VERIFY_EMAIL_TOKEN;
  private static final String TEST_VERIFY_LOGIN_DEVICE_LINK =
      TEST_AUTH_BASE_URI
          + TEST_VERIFY_LOGIN_DEVICE_PATH
          + "?token="
          + TEST_VERIFY_LOGIN_DEVICE_TOKEN;
  private static final String TEST_FORGOT_PASSWORD_LINK =
      TEST_FORGOT_PASSWORD_URL + "?token=" + TEST_FORGOT_PASSWORD_TOKEN;
  private static final String TEST_RENDERED_HTML = "<html>rendered</html>";

  @Mock private EmailTemplateSupport emailTemplateSupport;

  private AuthEmailFactory authEmailFactory;

  @BeforeEach
  void setUp() {
    when(emailTemplateSupport.readTemplate(any(), eq(VERIFY_EMAIL_TEMPLATE_NAME)))
        .thenReturn(TEST_VERIFY_EMAIL_TEMPLATE);
    when(emailTemplateSupport.readTemplate(any(), eq(VERIFY_LOGIN_DEVICE_TEMPLATE_NAME)))
        .thenReturn(TEST_VERIFY_LOGIN_TEMPLATE);
    when(emailTemplateSupport.readTemplate(any(), eq(FORGOT_PASSWORD_TEMPLATE_NAME)))
        .thenReturn(TEST_FORGOT_PASSWORD_TEMPLATE);

    authEmailFactory =
        new AuthEmailFactory(
            emailTemplateSupport,
            URI.create(TEST_AUTH_BASE_URI + TEST_VERIFY_REGISTRATION_PATH),
            URI.create(TEST_AUTH_BASE_URI + TEST_VERIFY_LOGIN_DEVICE_PATH),
            URI.create(TEST_FORGOT_PASSWORD_URL),
            new ByteArrayResource(new byte[0]),
            new ByteArrayResource(new byte[0]),
            new ByteArrayResource(new byte[0]));
  }

  @Test
  void buildVerifyEmailMessageBuildsVerificationEmail() {
    when(emailTemplateSupport.buildVerificationLink(
            URI.create(TEST_AUTH_BASE_URI + TEST_VERIFY_REGISTRATION_PATH),
            TEST_VERIFY_EMAIL_TOKEN))
        .thenReturn(TEST_VERIFY_EMAIL_LINK);
    when(emailTemplateSupport.renderHtml(
            TEST_VERIFY_EMAIL_TEMPLATE, TEST_VERIFY_EMAIL_LINK, "24 hours"))
        .thenReturn(TEST_RENDERED_HTML);

    EmailMessageDto emailMessage =
        authEmailFactory.buildVerifyEmailMessage(
            TEST_EMAIL_LAURA, TEST_VERIFY_EMAIL_TOKEN, TEST_VERIFY_EMAIL_TOKEN_TTL_SECONDS);

    assertEquals(TEST_EMAIL_LAURA, emailMessage.to());
    assertEquals("Verify your email", emailMessage.subject());
    assertEquals(TEST_RENDERED_HTML, emailMessage.html());
    assertEquals(
        "Verify your email by visiting "
            + TEST_VERIFY_EMAIL_LINK
            + ". This link will expire in 24 hours.",
        emailMessage.text());
    verify(emailTemplateSupport)
        .renderHtml(TEST_VERIFY_EMAIL_TEMPLATE, TEST_VERIFY_EMAIL_LINK, "24 hours");
  }

  @Test
  void buildVerifyLoginDeviceEmailMessageBuildsDeviceVerificationEmail() {
    when(emailTemplateSupport.buildVerificationLink(
            URI.create(TEST_AUTH_BASE_URI + TEST_VERIFY_LOGIN_DEVICE_PATH),
            TEST_VERIFY_LOGIN_DEVICE_TOKEN))
        .thenReturn(TEST_VERIFY_LOGIN_DEVICE_LINK);
    when(emailTemplateSupport.renderHtml(
            TEST_VERIFY_LOGIN_TEMPLATE, TEST_VERIFY_LOGIN_DEVICE_LINK, "30 minutes"))
        .thenReturn(TEST_RENDERED_HTML);

    EmailMessageDto emailMessage =
        authEmailFactory.buildVerifyLoginDeviceEmailMessage(
            TEST_EMAIL_LAURA,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN,
            TEST_VERIFY_LOGIN_DEVICE_TOKEN_TTL_SECONDS);

    assertEquals(TEST_EMAIL_LAURA, emailMessage.to());
    assertEquals("Confirm your login", emailMessage.subject());
    assertEquals(TEST_RENDERED_HTML, emailMessage.html());
    assertEquals(
        "We detected a login attempt to your Tyche Wealth account. Confirm it by visiting "
            + TEST_VERIFY_LOGIN_DEVICE_LINK
            + ". This link will expire in 30 minutes. If this wasn't you, change your password.",
        emailMessage.text());
    verify(emailTemplateSupport)
        .renderHtml(TEST_VERIFY_LOGIN_TEMPLATE, TEST_VERIFY_LOGIN_DEVICE_LINK, "30 minutes");
  }

  @Test
  void buildForgotPasswordEmailMessageBuildsResetPasswordEmail() {
    when(emailTemplateSupport.buildVerificationLink(
            URI.create(TEST_FORGOT_PASSWORD_URL), TEST_FORGOT_PASSWORD_TOKEN))
        .thenReturn(TEST_FORGOT_PASSWORD_LINK);
    when(emailTemplateSupport.renderHtml(
            TEST_FORGOT_PASSWORD_TEMPLATE, TEST_FORGOT_PASSWORD_LINK, "1 hour"))
        .thenReturn(TEST_RENDERED_HTML);

    EmailMessageDto emailMessage =
        authEmailFactory.buildForgotPasswordEmailMessage(
            TEST_EMAIL_LAURA, TEST_FORGOT_PASSWORD_TOKEN, TEST_FORGOT_PASSWORD_TOKEN_TTL_SECONDS);

    assertEquals(TEST_EMAIL_LAURA, emailMessage.to());
    assertEquals("Reset your password", emailMessage.subject());
    assertEquals(TEST_RENDERED_HTML, emailMessage.html());
    assertEquals(
        "Reset your Tyche Wealth password by visiting "
            + TEST_FORGOT_PASSWORD_LINK
            + ". This link will expire in 1 hour.",
        emailMessage.text());
    verify(emailTemplateSupport)
        .renderHtml(TEST_FORGOT_PASSWORD_TEMPLATE, TEST_FORGOT_PASSWORD_LINK, "1 hour");
  }
}

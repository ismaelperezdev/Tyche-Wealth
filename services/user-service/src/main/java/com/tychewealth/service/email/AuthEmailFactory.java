package com.tychewealth.service.email;

import static com.tychewealth.constants.EmailConstants.FORGOT_PASSWORD_SUBJECT;
import static com.tychewealth.constants.EmailConstants.FORGOT_PASSWORD_TEMPLATE_NAME;
import static com.tychewealth.constants.EmailConstants.LINK_EXPIRATION_TEXT;
import static com.tychewealth.constants.EmailConstants.VERIFY_EMAIL_SUBJECT;
import static com.tychewealth.constants.EmailConstants.VERIFY_EMAIL_TEMPLATE_NAME;
import static com.tychewealth.constants.EmailConstants.VERIFY_LOGIN_DEVICE_TEMPLATE_NAME;
import static com.tychewealth.constants.EmailConstants.VERIFY_LOGIN_SUBJECT;
import static com.tychewealth.utils.Utils.formatExpirationText;

import com.tychewealth.dto.email.request.EmailMessageDto;
import com.tychewealth.service.email.support.EmailTemplateSupport;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Builds email messages for authentication-related user workflows.
 *
 * <p>Loads the configured HTML templates once, creates registration, trusted-device, and password
 * recovery links, and combines them with localized subjects, expiration text, HTML content, and
 * plain-text fallbacks returned as {@link EmailMessageDto} instances.
 */
@Component
public class AuthEmailFactory {

  private final EmailTemplateSupport emailTemplateSupport;
  private final URI verifyRegistrationUri;
  private final URI verifyLoginDeviceUri;
  private final URI forgotPasswordUri;
  private final String verifyEmailTemplate;
  private final String verifyLoginDeviceTemplate;
  private final String forgotPasswordTemplate;

  public AuthEmailFactory(
      EmailTemplateSupport emailTemplateSupport,
      URI verifyRegistrationUri,
      URI verifyLoginDeviceUri,
      URI forgotPasswordUri,
      @Value("classpath:templates/email/verify-email.html") Resource verifyEmailTemplate,
      @Value("classpath:templates/email/verify-login-device.html")
          Resource verifyLoginDeviceTemplate,
      @Value("classpath:templates/email/forgot_password.html") Resource forgotPasswordTemplate) {

    this.emailTemplateSupport = emailTemplateSupport;
    this.verifyRegistrationUri = verifyRegistrationUri;
    this.verifyLoginDeviceUri = verifyLoginDeviceUri;
    this.forgotPasswordUri = forgotPasswordUri;

    this.verifyEmailTemplate =
        emailTemplateSupport.readTemplate(verifyEmailTemplate, VERIFY_EMAIL_TEMPLATE_NAME);

    this.verifyLoginDeviceTemplate =
        emailTemplateSupport.readTemplate(
            verifyLoginDeviceTemplate, VERIFY_LOGIN_DEVICE_TEMPLATE_NAME);

    this.forgotPasswordTemplate =
        emailTemplateSupport.readTemplate(forgotPasswordTemplate, FORGOT_PASSWORD_TEMPLATE_NAME);
  }

  public EmailMessageDto buildVerifyEmailMessage(
      String email, String verificationToken, long expiresInSeconds) {
    String verificationLink =
        emailTemplateSupport.buildVerificationLink(verifyRegistrationUri, verificationToken);
    String expirationText = formatExpirationText(expiresInSeconds);
    return new EmailMessageDto(
        email,
        VERIFY_EMAIL_SUBJECT,
        emailTemplateSupport.renderHtml(verifyEmailTemplate, verificationLink, expirationText),
        "Verify your email by visiting "
            + verificationLink
            + LINK_EXPIRATION_TEXT
            + expirationText
            + ".");
  }

  public EmailMessageDto buildVerifyLoginDeviceEmailMessage(
      String email, String verificationToken, long expiresInSeconds) {
    String verificationLink =
        emailTemplateSupport.buildVerificationLink(verifyLoginDeviceUri, verificationToken);
    String expirationText = formatExpirationText(expiresInSeconds);
    return new EmailMessageDto(
        email,
        VERIFY_LOGIN_SUBJECT,
        emailTemplateSupport.renderHtml(
            verifyLoginDeviceTemplate, verificationLink, expirationText),
        "We detected a login attempt to your Tyche Wealth account. Confirm it by visiting "
            + verificationLink
            + LINK_EXPIRATION_TEXT
            + expirationText
            + ". If this wasn't you, change your password.");
  }

  public EmailMessageDto buildForgotPasswordEmailMessage(
      String email, String token, long expiresInSeconds) {
    String verificationLink = emailTemplateSupport.buildVerificationLink(forgotPasswordUri, token);
    String expirationText = formatExpirationText(expiresInSeconds);
    return new EmailMessageDto(
        email,
        FORGOT_PASSWORD_SUBJECT,
        emailTemplateSupport.renderHtml(forgotPasswordTemplate, verificationLink, expirationText),
        "Reset your Tyche Wealth password by visiting "
            + verificationLink
            + LINK_EXPIRATION_TEXT
            + expirationText
            + ".");
  }
}

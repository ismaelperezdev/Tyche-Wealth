package com.tychewealth.service.helper.email;

import com.tychewealth.service.email.EmailMessage;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class RegisterEmailHelper {

  private static final String VERIFY_EMAIL_SUBJECT = "Verify your email";
  private static final String VERIFY_EMAIL_LINK_PLACEHOLDER = "{{verification_link}}";

  private final String verifyEmailUrl;
  private final String cachedVerifyEmailTemplate;

  public RegisterEmailHelper(
      URI verifyRegistrationUri,
      @Value("classpath:templates/email/verify-email.html") Resource verifyEmailTemplate) {
    this.verifyEmailUrl = verifyRegistrationUri.toString();
    try {
      this.cachedVerifyEmailTemplate =
          StreamUtils.copyToString(verifyEmailTemplate.getInputStream(), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to load verify email template", ex);
    }
  }

  public EmailMessage buildVerifyEmailMessage(
      String email, String verificationToken, long expiresInSeconds) {
    String verificationLink = buildVerificationLink(verificationToken);
    return new EmailMessage(
        email,
        VERIFY_EMAIL_SUBJECT,
        renderHtml(verificationLink),
        buildText(verificationLink, expiresInSeconds));
  }

  private String buildVerificationLink(String token) {
    return UriComponentsBuilder.fromUriString(verifyEmailUrl)
        .queryParam("token", token)
        .build(true)
        .toUriString();
  }

  private String renderHtml(String verificationLink) {
    return cachedVerifyEmailTemplate.replace(VERIFY_EMAIL_LINK_PLACEHOLDER, verificationLink);
  }

  private String buildText(String verificationLink, long expiresInSeconds) {
    long expiresInMinutes = (expiresInSeconds + 59) / 60;
    return "Verify your email by visiting "
        + verificationLink
        + ". This link will expire in "
        + expiresInMinutes
        + " minutes.";
  }
}

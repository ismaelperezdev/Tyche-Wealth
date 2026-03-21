package com.tychewealth.service.helper.email;

import com.tychewealth.service.email.EmailMessage;
import java.io.IOException;
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
  private final Resource verifyEmailTemplate;

  public RegisterEmailHelper(
      @Value(
              "${app.auth.verify-registration-url:http://localhost:8080/tyche-wealth/user-service/v1/auth/verify-registration}")
          String verifyEmailUrl,
      @Value("classpath:templates/email/verify-email.html") Resource verifyEmailTemplate) {
    this.verifyEmailUrl = verifyEmailUrl;
    this.verifyEmailTemplate = verifyEmailTemplate;
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
    try {
      String template =
          StreamUtils.copyToString(verifyEmailTemplate.getInputStream(), StandardCharsets.UTF_8);
      return template.replace(VERIFY_EMAIL_LINK_PLACEHOLDER, verificationLink);
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to load verify email template", ex);
    }
  }

  private String buildText(String verificationLink, long expiresInSeconds) {
    long expiresInMinutes = expiresInSeconds / 60;
    return "Verify your email by visiting "
        + verificationLink
        + ". This link will expire in "
        + expiresInMinutes
        + " minutes.";
  }
}

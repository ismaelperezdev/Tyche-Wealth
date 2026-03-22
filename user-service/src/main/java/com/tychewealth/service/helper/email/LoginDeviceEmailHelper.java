package com.tychewealth.service.helper.email;

import com.tychewealth.service.email.EmailMessage;
import com.tychewealth.utils.Utils;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class LoginDeviceEmailHelper {

  private static final String VERIFY_LOGIN_SUBJECT = "Confirm your login";
  private static final String VERIFY_LOGIN_LINK_PLACEHOLDER = "{{verification_link}}";
  private static final String VERIFY_LOGIN_EXPIRATION_PLACEHOLDER = "{{expiration_text}}";

  private final String verifyLoginDeviceUrl;
  private final String cachedLoginDeviceTemplate;

  public LoginDeviceEmailHelper(
      URI verifyLoginDeviceUri,
      @Value("classpath:templates/email/verify-login-device.html")
          Resource verifyLoginDeviceTemplate) {
    this.verifyLoginDeviceUrl = verifyLoginDeviceUri.toString();
    try {
      this.cachedLoginDeviceTemplate =
          StreamUtils.copyToString(
              verifyLoginDeviceTemplate.getInputStream(), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to load login device email template", ex);
    }
  }

  public EmailMessage buildVerifyLoginDeviceEmailMessage(
      String email, String verificationToken, long expiresInSeconds) {
    String verificationLink = buildVerificationLink(verificationToken);
    String expirationText = Utils.formatExpirationText(expiresInSeconds);
    return new EmailMessage(
        email,
        VERIFY_LOGIN_SUBJECT,
        renderHtml(verificationLink, expirationText),
        buildText(verificationLink, expirationText));
  }

  private String buildVerificationLink(String token) {
    return UriComponentsBuilder.fromUriString(verifyLoginDeviceUrl)
        .queryParam("token", token)
        .build(true)
        .toUriString();
  }

  private String renderHtml(String verificationLink, String expirationText) {
    return cachedLoginDeviceTemplate
        .replace(VERIFY_LOGIN_LINK_PLACEHOLDER, verificationLink)
        .replace(VERIFY_LOGIN_EXPIRATION_PLACEHOLDER, expirationText);
  }

  private String buildText(String verificationLink, String expirationText) {
    return "We detected a login attempt to your Tyche Wealth account. Confirm it by visiting "
        + verificationLink
        + ". This link will expire in "
        + expirationText
        + ". If this wasn't you, change your password.";
  }
}

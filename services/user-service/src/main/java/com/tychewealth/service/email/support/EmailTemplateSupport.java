package com.tychewealth.service.email.support;

import static com.tychewealth.constants.EmailConstants.EXPIRATION_TEXT_PLACEHOLDER;
import static com.tychewealth.constants.EmailConstants.VERIFICATION_LINK_PLACEHOLDER;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Provides shared operations for rendering authentication email templates.
 *
 * <p>Builds tokenized verification links, replaces template placeholders with workflow values, and
 * loads UTF-8 template resources. Template I/O failures are converted into an IllegalStateException
 * so application startup or message construction fails explicitly.
 */
@Component
public class EmailTemplateSupport {

  public String buildVerificationLink(URI baseUri, String token) {
    return UriComponentsBuilder.fromUriString(baseUri.toString())
        .queryParam("token", token)
        .build(true)
        .toUriString();
  }

  public String renderHtml(String template, String verificationLink, String expirationText) {
    return template
        .replace(VERIFICATION_LINK_PLACEHOLDER, verificationLink)
        .replace(EXPIRATION_TEXT_PLACEHOLDER, expirationText);
  }

  public String readTemplate(Resource resource, String templateName) {
    try (var inputStream = resource.getInputStream()) {
      return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to load " + templateName + " template", ex);
    }
  }
}

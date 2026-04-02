package com.tychewealth.constants;

public final class EmailConstants {

  public static final String VERIFY_EMAIL_SUBJECT = "Verify your email";
  public static final String VERIFY_LOGIN_SUBJECT = "Confirm your login";
  public static final String FORGOT_PASSWORD_SUBJECT = "Reset your password";
  public static final String VERIFY_EMAIL_TEMPLATE_NAME = "verify email";
  public static final String VERIFY_LOGIN_DEVICE_TEMPLATE_NAME = "login device email";
  public static final String FORGOT_PASSWORD_TEMPLATE_NAME = "forgot password email";
  public static final String VERIFICATION_LINK_PLACEHOLDER = "{{verification_link}}";
  public static final String EXPIRATION_TEXT_PLACEHOLDER = "{{expiration_text}}";
  public static final String LINK_EXPIRATION_TEXT = ". This link will expire in ";

  private EmailConstants() {}
}

package com.tychewealth.constants;

public final class AuthConstants {

  public static final String EMAIL_CONSTRAINT = "uk_users_email";
  public static final String USERNAME_CONSTRAINT = "uk_users_username";

  public static final String LOGIN_PASSWORD_POLICY =
      "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).{8,72}$";

  public static final int BCRYPT_MAX_PASSWORD_BYTES = 72;
  public static final String AUTHORIZATION_HEADER = "Authorization";
  public static final String TOKEN_TYPE_BEARER = "Bearer";
  public static final String TOKEN_TYPE_BEARER_PREFIX = TOKEN_TYPE_BEARER + " ";
  public static final int REFRESH_TOKEN_BYTE_LENGTH = 32;

  private AuthConstants() {}
}

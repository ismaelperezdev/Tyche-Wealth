package com.tychewealth.constants;

public final class AuthConstants {

  public static final String EMAIL_CONSTRAINT = "uk_users_email";
  public static final String USERNAME_CONSTRAINT = "uk_users_username";

  public static final String LOGIN_PASSWORD_POLICY =
      "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^\\w\\s]).{8,72}$";

  public static final int BCRYPT_MAX_PASSWORD_BYTES = 72;
  public static final String TOKEN_TYPE_BEARER = "Bearer";
  public static final int REFRESH_TOKEN_BYTE_LENGTH = 32;

  public static final String LOGIN_RATE_LIMIT_MESSAGE = "Too many login requests";
  public static final String REGISTER_RATE_LIMIT_MESSAGE = "Too many register requests";
  public static final String REFRESH_RATE_LIMIT_MESSAGE = "Too many refresh token requests";

  public static final String METRIC_AUTH_REGISTER_REQUESTS = "tyche.auth.register.requests";
  public static final String METRIC_AUTH_REGISTER_SUCCESS = "tyche.auth.register.success";
  public static final String METRIC_AUTH_REGISTER_FAILURE = "tyche.auth.register.failure";
  public static final String METRIC_AUTH_REGISTER_RATE_LIMITED = "tyche.auth.register.rate_limited";
  public static final String METRIC_AUTH_REGISTER_CONFLICT = "tyche.auth.register.conflict";

  public static final String METRIC_AUTH_LOGIN_REQUESTS = "tyche.auth.login.requests";
  public static final String METRIC_AUTH_LOGIN_SUCCESS = "tyche.auth.login.success";
  public static final String METRIC_AUTH_LOGIN_FAILURE = "tyche.auth.login.failure";
  public static final String METRIC_AUTH_LOGIN_RATE_LIMITED = "tyche.auth.login.rate_limited";
  public static final String METRIC_AUTH_LOGIN_INVALID_CREDENTIALS =
      "tyche.auth.login.invalid_credentials";

  public static final String METRIC_AUTH_REFRESH_REQUESTS = "tyche.auth.refresh.requests";
  public static final String METRIC_AUTH_REFRESH_SUCCESS = "tyche.auth.refresh.success";
  public static final String METRIC_AUTH_REFRESH_FAILURE = "tyche.auth.refresh.failure";
  public static final String METRIC_AUTH_REFRESH_RATE_LIMITED = "tyche.auth.refresh.rate_limited";
  public static final String METRIC_AUTH_REFRESH_TOKEN_ISSUED = "tyche.auth.refresh_token.issued";
  public static final String METRIC_AUTH_REFRESH_TOKEN_REVOKED = "tyche.auth.refresh_token.revoked";

  /**
 * Prevents instantiation of this utility class.
 */
private AuthConstants() {}
}

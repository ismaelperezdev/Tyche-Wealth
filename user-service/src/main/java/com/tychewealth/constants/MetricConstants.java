package com.tychewealth.constants;

public final class MetricConstants {

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

  public static final String METRIC_USER_RETRIEVE_REQUESTS = "tyche.user.retrieve.requests";
  public static final String METRIC_USER_RETRIEVE_SUCCESS = "tyche.user.retrieve.success";

  public static final String METRIC_USER_UPDATE_REQUESTS = "tyche.user.update.requests";
  public static final String METRIC_USER_UPDATE_SUCCESS = "tyche.user.update.success";

  public static final String METRIC_USER_UPDATE_PASSWORD_REQUESTS =
      "tyche.user.update_password.requests";
  public static final String METRIC_USER_UPDATE_PASSWORD_SUCCESS =
      "tyche.user.update_password.success";

  public static final String METRIC_USER_DELETE_REQUESTS = "tyche.user.delete.requests";
  public static final String METRIC_USER_DELETE_SUCCESS = "tyche.user.delete.success";

  public static final String METRIC_USER_UNAUTHORIZED = "tyche.user.unauthorized";
  public static final String METRIC_USER_NOT_FOUND = "tyche.user.not_found";
  public static final String METRIC_USER_USERNAME_CONFLICT = "tyche.user.username_conflict";
  public static final String METRIC_USER_CURRENT_PASSWORD_INVALID =
      "tyche.user.current_password_invalid";
  public static final String METRIC_USER_NEW_PASSWORD_REUSED = "tyche.user.new_password_reused";

  private MetricConstants() {}
}

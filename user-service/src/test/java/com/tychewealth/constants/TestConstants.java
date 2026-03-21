package com.tychewealth.constants;

public final class TestConstants {

  public static final String TEST_EMAIL_LAURA = "laura.gomez@tychewealth.com";
  public static final String TEST_EMAIL_VALID = "valid@tychewealth.com";
  public static final String TEST_EMAIL_INVALID = "not-an-email";
  public static final String TEST_USERNAME_LAURA = "lauragomez";
  public static final String TEST_USERNAME_VALID = "validuser";
  public static final String TEST_USERNAME_TOO_SHORT = "ab";
  public static final String TEST_PASSWORD_VALID = "Secret123!";
  public static final String TEST_PASSWORD_INVALID = "Wrong123!";
  public static final String TEST_PASSWORD_NEW_VALID = "NewSecret456!";
  public static final String TEST_PASSWORD_TOO_SHORT = "short1!";
  public static final String TEST_PASSWORD_LOWERCASE_ONLY = "alllowercase1!";
  public static final String TEST_PASSWORD_CONFIRM_MISMATCH = "Mismatch456!";
  public static final String TEST_FIELD_CURRENT_PASSWORD = "currentPassword";
  public static final String TEST_FIELD_NEW_PASSWORD = "newPassword";
  public static final String TEST_FIELD_CONFIRM_NEW_PASSWORD = "confirmNewPassword";
  public static final String TEST_FIELD_USERNAME = "username";

  public static final String TEST_UPDATE_USERNAME_REQUEST = "AfterUpdate";
  public static final String TEST_UPDATE_USERNAME_NORMALIZED = "afterupdate";
  public static final String TEST_OCCUPIED_USERNAME = "occupiedname";
  public static final String TEST_OTHER_EMAIL = "otro.usuario@tychewealth.com";

  public static final String TEST_REFRESH_TOKEN_MISSING = "missing-token";
  public static final String TEST_REFRESH_TOKEN_EXISTING = "existing-refresh-token";
  public static final String TEST_REFRESH_TOKEN_REVOKED = "revoked-refresh-token";
  public static final String TEST_REFRESH_TOKEN_EXPIRED = "expired-refresh-token";
  public static final String TEST_REFRESH_TOKEN_METRICS = "metrics-refresh-token";
  public static final String TEST_REFRESH_TOKEN_PEPPER = "test-refresh-token-pepper";
  public static final String TEST_RESEND_BASE_URL = "https://api.resend.com";
  public static final String TEST_RESEND_API_KEY = "re_test_key";
  public static final String TEST_RESEND_FROM = "Tyche Wealth <auth@tyche-wealth.com>";
  public static final String TEST_EMAIL_SUBJECT_VERIFY = "Verify your email";
  public static final String TEST_EMAIL_HTML_BODY = "<p>Hello</p>";
  public static final String TEST_EMAIL_TEXT_BODY = "Hello";
  public static final int TEST_EMAIL_DAILY_LIMIT = 2;

  public static final String TEST_PROMETHEUS_USERNAME = "prometheus";
  public static final String TEST_PROMETHEUS_PASSWORD = "secret";
  public static final String TEST_MISSING_USERNAME = "missing";
  public static final String TEST_PROMETHEUS_ROLE = "ROLE_PROMETHEUS";
  public static final String TEST_PROMETHEUS_CREDENTIALS_ERROR =
      "Prometheus username/password not configured";
  public static final String TEST_HEADER_X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
  public static final String TEST_HEADER_X_FRAME_OPTIONS = "X-Frame-Options";
  public static final String TEST_HEADER_REFERRER_POLICY = "Referrer-Policy";
  public static final String TEST_HEADER_STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
  public static final String TEST_ATTACKER_BASIC_TOKEN = "Basic attacker-token";
  public static final String TEST_TAMPERED_TOKEN_SUFFIX = "tampered";

  private TestConstants() {}
}

package com.tychewealth.constants;

public final class TestConstants {

  public static final String TEST_EMAIL_LAURA = "laura.gomez@tychewealth.com";
  public static final String TEST_EMAIL_VALID = "valid@tychewealth.com";
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

  public static final String TEST_UPDATE_USERNAME_REQUEST = "AfterUpdate";
  public static final String TEST_OCCUPIED_USERNAME = "occupiedname";
  public static final String TEST_OTHER_EMAIL = "otro.usuario@tychewealth.com";

  public static final String TEST_REFRESH_TOKEN_MISSING = "missing-token";
  public static final String TEST_REFRESH_TOKEN_EXISTING = "existing-refresh-token";
  public static final String TEST_REFRESH_TOKEN_PEPPER = "test-refresh-token-pepper";
  public static final String TEST_RESEND_BASE_URL = "https://api.resend.com";
  public static final String TEST_EMAIL_SUBJECT_VERIFY = "Verify your email";
  public static final String TEST_EMAIL_HTML_BODY = "<p>Hello</p>";

  public static final String TEST_PROMETHEUS_USERNAME = "prometheus";
  public static final String TEST_PROMETHEUS_PASSWORD = "secret";
  public static final long TEST_USER_ID = 42L;
  public static final String TEST_JWT_SECRET =
      "0123456789012345678901234567890123456789012345678901234567890123";
  public static final long TEST_ACCESS_TOKEN_TTL_SECONDS = 900L;
  public static final long TEST_VERIFY_EMAIL_TOKEN_TTL_SECONDS = 86400L;
  public static final long TEST_VERIFY_LOGIN_DEVICE_TOKEN_TTL_SECONDS = 1800L;
  public static final long TEST_FORGOT_PASSWORD_TOKEN_TTL_SECONDS = 3600L;
  public static final String TEST_ACCESS_TOKEN = "access-token";
  public static final String TEST_ACCESS_TOKEN_JTI = "jti-123";
  public static final String TEST_BEARER_ACCESS_TOKEN = "Bearer access-token";
  public static final String TEST_VERIFY_REGISTRATION_PATH = "/verify-registration";
  public static final String TEST_TRUSTED_DEVICE_COOKIE_NAME = "trusted_device";
  public static final String TEST_RATE_LIMIT_STORE_UNAVAILABLE = "store unavailable";

  private TestConstants() {}
}

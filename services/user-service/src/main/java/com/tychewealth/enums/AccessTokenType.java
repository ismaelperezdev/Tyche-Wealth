package com.tychewealth.enums;

/**
 * Identifies the purpose of an access-token-like credential issued by the authentication flow.
 *
 * <p>The token purpose is embedded in validation rules so credentials issued for API access, email
 * verification, trusted-device verification, and password recovery cannot be used interchangeably.
 */
public enum AccessTokenType {
  ACCESS,
  VERIFY_EMAIL,
  VERIFY_LOGIN_DEVICE,
  FORGOT_PASSWORD
}

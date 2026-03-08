package com.tychewealth.utils;

import com.tychewealth.dto.user.request.UserCreateRequestDto;
import java.util.Locale;

public final class AuthInputNormalizer {

  private AuthInputNormalizer() {}

  public static void normalizeRegisterIdentifiers(UserCreateRequestDto register) {
    if (register == null) {
      return;
    }
    register.setEmail(canonicalize(register.getEmail()));
    register.setUsername(canonicalize(register.getUsername()));
  }

  private static String canonicalize(String value) {
    if (value == null) {
      return null;
    }
    return value.trim().toLowerCase(Locale.ROOT);
  }
}

package org.spacehub.security;

import org.springframework.stereotype.Component;

@Component
public class PasswordValidator {

  public static String getValidationMessage(String password) {
    if (password.length() < 8) return "Password must be at least 8 characters long";
    if (!password.matches(".*[A-Z].*")) return "Password must contain at least one uppercase letter";
    if (!password.matches(".*[a-z].*")) return "Password must contain at least one lowercase letter";
    if (!password.matches(".*[0-9].*")) return "Password must contain at least one digit";
    if (!password.matches(".*[@#$%^&+=!].*")) return "Password must contain at least one special character";
    if (password.matches(".*\\s.*")) return "Password must not contain spaces";
    return "Valid";
  }
}

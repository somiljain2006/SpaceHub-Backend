package org.spacehub.security;

import org.springframework.stereotype.Component;

@Component
public class EmailValidator {
  public String normalize(String email) {
    if (email == null) {
      return null;
    }
    return email.trim().toLowerCase();
  }
}

package org.spacehub.security;

import org.springframework.stereotype.Component;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Component
public class EmailValidator implements Predicate<String> {

  private static final String EMAIL_REGEX =
          "^(?=.{1,64}@)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

  private static final Pattern PATTERN = Pattern.compile(EMAIL_REGEX);

  @Override
  public boolean test(String email) {
    if (email == null) return false;
    email = email.trim().toLowerCase();
    return PATTERN.matcher(email).matches();
  }

  public String normalize(String email) {
    return email == null ? null : email.trim().toLowerCase();
  }
}

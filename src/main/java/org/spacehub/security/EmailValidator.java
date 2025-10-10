package org.spacehub.security;

import org.springframework.stereotype.Component;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Component
public class EmailValidator implements Predicate<String> {

  private static final String emailRegex =
    "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@" +
      "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";

  private static final Pattern pattern = Pattern.compile(emailRegex);

  public boolean test(String email) {
    if (email == null) {
      return false;
    }
    return pattern.matcher(email).matches();
  }
}

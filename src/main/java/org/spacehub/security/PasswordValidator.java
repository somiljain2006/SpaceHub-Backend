package org.spacehub.security;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PasswordValidator {

    private static final String PASSWORD_PATTERN =
            "^(?=.*[0-9])" +
                    "(?=.*[a-z])" +
                    "(?=.*[A-Z])" +
                    "(?=.*[@#$%^&+=!])" +
                    "(?=\\S+$).{8,}$";

    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    public boolean isValid(String password) {
        return pattern.matcher(password).matches();
    }

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

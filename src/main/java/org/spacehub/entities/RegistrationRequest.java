package org.spacehub.entities;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationRequest {

  @NotBlank(message = "First name is required")
  @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
  @Pattern(regexp = "^[A-Za-z]+$", message = "First name must contain only letters and no spaces")
  private String firstName;

  @NotBlank(message = "Last name is required")
  @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
  @Pattern(regexp = "^[A-Za-z]+$", message = "First name must contain only letters and no spaces")
  private String lastName;

  @Email(message = "Invalid email format")
  @NotBlank(message = "Email is required")
  @Size(max = 254, message = "Email must not exceed 254 characters")
  private String email;

  @NotBlank(message = "Password is required")
  @Pattern(
    regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@#$%^&+=!])(?!.*\\s).+$",
    message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, " +
      "one special character, and no spaces"
  )
  private String password;
}

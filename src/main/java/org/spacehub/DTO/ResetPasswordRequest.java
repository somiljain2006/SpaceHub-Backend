package org.spacehub.DTO;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.NoArgsConstructor;


@Getter
@Setter
public class ResetPasswordRequest {

  @NotBlank(message = "Email is required")
  @Email(message = "Email should be valid")
  private String email;

  @NotBlank(message = "New password is required")
  private String newPassword;

  @NotBlank(message = "Temporary token is required")
  private String tempToken;

  public ResetPasswordRequest(String email, String newPassword, String tempToken) {
    this.email = email;
    this.newPassword = newPassword;
    this.tempToken = tempToken;
  }
}

package org.spacehub.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class ValidateForgotOtpRequest {

  @NotBlank(message = "Email is required")
  @Email(message = "Email should be valid")
  private String email;

  @NotBlank(message = "OTP is required")
  private String otp;

  public ValidateForgotOtpRequest(String email, String otp) {
    this.email = email;
    this.otp = otp;
  }
}

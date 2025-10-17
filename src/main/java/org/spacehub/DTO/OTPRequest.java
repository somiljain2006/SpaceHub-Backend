package org.spacehub.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.spacehub.entities.OtpType;

@Data
public class OTPRequest {

  @NotBlank(message = "Email is required")
  @Email(message = "Email should be valid")
  private String email;

  @NotBlank(message = "OTP is required")
  private String otp;

  @NotNull(message = "OTP type is required")
  private OtpType type;

}

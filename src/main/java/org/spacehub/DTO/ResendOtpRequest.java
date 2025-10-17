package org.spacehub.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Service
@AllArgsConstructor
public class ResendOtpRequest {

  @NotBlank(message = "Email is required")
  @Email(message = "Email should be valid")
  private String email;

}

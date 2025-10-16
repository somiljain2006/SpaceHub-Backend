package org.spacehub.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResendForgotOtpRequest {

  @NotBlank(message = "Temporary token is required")
  private String tempToken;

}

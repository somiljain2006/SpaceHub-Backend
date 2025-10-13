package org.spacehub.DTO;

import lombok.Data;

@Data
public class ResetPasswordRequest {
  private String email;
  private String newPassword;
}

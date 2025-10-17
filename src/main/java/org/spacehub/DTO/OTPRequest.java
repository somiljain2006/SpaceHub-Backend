package org.spacehub.DTO;

import lombok.Data;
import org.spacehub.entities.OtpType;

@Data
public class OTPRequest {

  private String email;
  private String otp;
  private OtpType type;

}
package org.spacehub.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResendForgotOtpRequest {
  private String tempToken;
}
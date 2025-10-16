package org.spacehub.DTO;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

@NoArgsConstructor
@Getter
@Service
public class ResendOtpRequest {
  private String email;

  public ResendOtpRequest(String email) {
    this.email = email;
  }
}

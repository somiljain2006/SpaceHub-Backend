package org.spacehub.DTO;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RefreshRequest {
  private String refreshToken;
  public RefreshRequest(String refreshToken) {
    this.refreshToken = refreshToken;
  }
}
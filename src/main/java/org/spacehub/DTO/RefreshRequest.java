package org.spacehub.DTO;

import lombok.Data;

@Data
public class RefreshRequest {
  private String refreshToken;
  public RefreshRequest(String refreshToken) {
    this.refreshToken = refreshToken;
  }
}

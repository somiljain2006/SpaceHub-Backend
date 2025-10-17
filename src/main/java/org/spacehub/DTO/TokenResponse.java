package org.spacehub.DTO;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Data
@Getter
@Setter
public class TokenResponse {

  @NonNull
  private String accessToken;

  @NonNull
  private String refreshToken;

  public TokenResponse(@NonNull String accessToken, @NonNull String refreshToken) {
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
  }
}

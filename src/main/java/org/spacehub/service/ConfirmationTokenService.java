package org.spacehub.service;

import lombok.AllArgsConstructor;
import org.spacehub.repository.ConfirmationTokenRepository;
import org.spacehub.security.ConfirmationToken;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ConfirmationTokenService {

  private final ConfirmationTokenRepository confirmationTokenRepository;

  public void saveConfirmationToken(ConfirmationToken token) {
    confirmationTokenRepository.save(token);
  }
}

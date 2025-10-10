package org.spacehub.service;

import org.spacehub.repository.ConfirmationTokenRepository;
import org.spacehub.security.ConfirmationToken;
import org.springframework.stereotype.Service;

@Service
public class ConfirmationTokenService {

  private final ConfirmationTokenRepository confirmationTokenRepository;

  public ConfirmationTokenService(ConfirmationTokenRepository confirmationTokenRepository) {
    this.confirmationTokenRepository = confirmationTokenRepository;
  }

  public void saveConfirmationToken(ConfirmationToken token) {
    confirmationTokenRepository.save(token);
  }
}

package org.spacehub.service;

import org.spacehub.entities.RefreshToken;
import org.spacehub.entities.User;
import org.spacehub.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class RefreshTokenService {

  private final RefreshTokenRepository refreshTokenRepository;
  private static final long refreshTokenDay = 7;

  public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
    this.refreshTokenRepository = refreshTokenRepository;
  }

  public RefreshToken createRefreshToken(User user) {
    Instant now = Instant.now();
    Instant expiresAt = now.plus(refreshTokenDay, ChronoUnit.DAYS);
    RefreshToken refreshToken = new RefreshToken(user, now, expiresAt);
    return refreshTokenRepository.save(refreshToken);
  }

  public Optional<RefreshToken> findByToken(String token) {
    return refreshTokenRepository.findByToken(token);
  }

  public void deleteByToken(String token) {
    refreshTokenRepository.deleteByToken(token);
  }

  public boolean isExpired(RefreshToken token) {
    return token.getExpiresAt().isBefore(Instant.now());
  }
}

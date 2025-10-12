package org.spacehub.service;

import org.spacehub.DTO.TokenResponse;
import org.spacehub.entities.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

@Service
public class VerificationService {

  private final UserNameService userNameService;
  private final AuthenticationManager authenticationManager;
  private final OTPService otpService;
  private final RefreshTokenService refreshTokenService;
  private final UserService userService;

  public VerificationService(UserNameService userNameService,
                             AuthenticationManager authenticationManager,
                             OTPService otpService, RefreshTokenService refreshTokenService,
                             UserService userService) {
    this.userNameService = userNameService;
    this.authenticationManager = authenticationManager;
    this.otpService = otpService;
    this.refreshTokenService = refreshTokenService;
    this.userService = userService;
  }

  public TokenResponse authenticateAndIssueTokens(User user) {
    Authentication authentication = authenticationManager
      .authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));

    if (!authentication.isAuthenticated()) {
      return null;
    }

    UserDetails principal = (UserDetails) authentication.getPrincipal();
    String accessToken = userNameService.generateToken(principal);
    User fullUser = (User) userService.loadUserByUsername(principal.getUsername());
    var refreshTokenEntity = refreshTokenService.createRefreshToken(fullUser);
    String refreshToken = refreshTokenEntity.getToken();
    otpService.sendOTP(user.getUsername());

    return new TokenResponse(accessToken, refreshToken);
  }
}

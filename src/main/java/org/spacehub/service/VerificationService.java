package org.spacehub.service;

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

  public VerificationService(UserNameService userNameService, AuthenticationManager authenticationManager, OTPService otpService) {
    this.userNameService = userNameService;
    this.authenticationManager = authenticationManager;
    this.otpService = otpService;
  }

  public String check(User user) {
    Authentication authentication = authenticationManager
      .authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));

    if (authentication.isAuthenticated()) {
      UserDetails principal = (UserDetails) authentication.getPrincipal();
      otpService.sendOTP(user.getUsername());
      return userNameService.generateToken(principal);
    } else {
      return null;
    }
  }
}


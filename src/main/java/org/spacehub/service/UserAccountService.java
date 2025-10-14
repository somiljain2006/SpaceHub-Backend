package org.spacehub.service;

import org.spacehub.DTO.*;
import org.spacehub.entities.ApiResponse;
import org.spacehub.entities.User;
import org.spacehub.entities.RegistrationRequest;
import org.spacehub.entities.OtpType;
import org.spacehub.security.EmailValidator;
import org.springframework.stereotype.Service;

@Service
public class UserAccountService {

  private final VerificationService verificationService;
  private final RegistrationService registrationService;
  private final EmailValidator emailValidator;
  private final OTPService otpService;
  private final UserService userService;
  private final RefreshTokenService refreshTokenService;

  public UserAccountService(VerificationService verificationService,
                            RegistrationService registrationService,
                            EmailValidator emailValidator,
                            OTPService otpService,
                            UserService userService,
                            RefreshTokenService refreshTokenService) {
    this.verificationService = verificationService;
    this.registrationService = registrationService;
    this.emailValidator = emailValidator;
    this.otpService = otpService;
    this.userService = userService;
    this.refreshTokenService = refreshTokenService;
  }

  public ApiResponse<TokenResponse> login(LoginRequest request) {
    String email = emailValidator.normalize(request.getEmail());

    if (!otpService.canSendOTP(email, OtpType.LOGIN)) {
      long secondsLeft = otpService.cooldownTime(email, OtpType.LOGIN);
      return new ApiResponse<>(400,
        "Please wait " + secondsLeft + " seconds before trying to login again.",
        null);
    }

    User user;
    try {
      user = userService.getUserByEmail(email);
      if (!user.getIsVerifiedRegistration()) {
        return new ApiResponse<>(400, "Register first", null);
      }
    } catch (Exception e) {
      return new ApiResponse<>(400, "User not found", null);
    }

    if (!verificationService.checkCredentials(user.getEmail(), request.getPassword())) {
      return new ApiResponse<>(400, "Invalid credentials", null);
    }

    otpService.sendOTP(email, OtpType.LOGIN);
    return new ApiResponse<>(200, "OTP sent for login verification", null);
  }

  public ApiResponse<String> register(RegistrationRequest request) {
    String email = emailValidator.normalize(request.getEmail());

    if (!otpService.canSendOTP(email, OtpType.REGISTRATION)) {
      long secondsLeft = otpService.cooldownTime(email, OtpType.REGISTRATION);
      return new ApiResponse<>(400,
        "Please wait " + secondsLeft + " seconds before registering again.", null);
    }

    request.setEmail(email);
    try {
      User user = registrationService.register(request);
      user.setIsVerifiedRegistration(false);
      userService.save(user);

      otpService.sendOTP(email, OtpType.REGISTRATION);
      return new ApiResponse<>(201,
        "OTP sent. Complete registration by validating OTP.", null);
    } catch (IllegalStateException e) {
      return new ApiResponse<>(400, e.getMessage(), null);
    } catch (Exception e) {
      return new ApiResponse<>(500, "Registration failed", null);
    }
  }

  public ApiResponse<?> validateOTP(OTPRequest request) {
    OtpType type = request.getType();
    boolean valid = otpService.validateOTP(request.getEmail(), request.getOtp(), type);
    if (!valid) {
      return new ApiResponse<>(400, "OTP is invalid or expired", null);
    }

    User user;
    try {
      user = userService.getUserByEmail(request.getEmail());
    } catch (Exception e) {
      return new ApiResponse<>(400, "User not found", null);
    }

    if (type == OtpType.REGISTRATION) {
      user.setIsVerifiedRegistration(true);
      userService.save(user);
      return new ApiResponse<>(200, "Registration verified successfully", null);
    } else if (type == OtpType.LOGIN) {
      user.setIsVerifiedLogin(true);
      userService.save(user);
      TokenResponse tokens = verificationService.generateTokens(user);
      return new ApiResponse<>(200, "Login verified successfully", tokens);
    } else if (type == OtpType.FORGOT_PASSWORD) {
      user.setIsVerifiedForgot(true);
      userService.save(user);
      return new ApiResponse<>(200, "OTP verified. You can reset your password now.", null);
    }

    return new ApiResponse<>(200, "OTP verified", null);
  }

  public ApiResponse<String> forgotPassword(String email) {
    String normalizedEmail = emailValidator.normalize(email);

    try {
      userService.getUserByEmail(normalizedEmail);
    } catch (Exception e) {
      return new ApiResponse<>(400, "User not found", null);
    }

    if (!otpService.canSendOTP(normalizedEmail, OtpType.FORGOT_PASSWORD)) {
      long secondsLeft = otpService.cooldownTime(normalizedEmail, OtpType.FORGOT_PASSWORD);
      return new ApiResponse<>(400,
        "Please wait " + secondsLeft + " seconds before requesting OTP again.", null);
    }

    otpService.sendOTP(normalizedEmail, OtpType.FORGOT_PASSWORD);
    return new ApiResponse<>(200, "OTP sent to your email", null);
  }

  public ApiResponse<String> resetPassword(ResetPasswordRequest request) {
    String email = emailValidator.normalize(request.getEmail());
    String newPassword = request.getNewPassword();

    User user;
    try {
      user = userService.getUserByEmail(email);
    } catch (Exception e) {
      return new ApiResponse<>(400, "User not found", null);
    }

    if (!user.getIsVerifiedForgot()) {
      return new ApiResponse<>(400, "OTP not verified for password reset", null);
    }

    userService.updatePassword(email, newPassword);
    user.setIsVerifiedForgot(false);
    userService.save(user);

    return new ApiResponse<>(200, "Password has been reset successfully", null);
  }

  public ApiResponse<String> logout(RefreshRequest request) {
    if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
      return new ApiResponse<>(400, "Refresh token is required", null);
    }

    String refreshToken = request.getRefreshToken();
    boolean deleted = refreshTokenService.deleteIfExists(refreshToken);

    if (!deleted) {
      return new ApiResponse<>(404, "Refresh token not found", null);
    }

    return new ApiResponse<>(200, "Logout successful", null);
  }
}


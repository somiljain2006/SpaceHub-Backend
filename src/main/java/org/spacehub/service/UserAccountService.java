package org.spacehub.service;

import org.spacehub.DTO.*;
import org.spacehub.entities.ApiResponse;
import org.spacehub.entities.User;
import org.spacehub.entities.RegistrationRequest;
import org.spacehub.entities.OtpType;
import org.spacehub.entities.UserRole;
import org.spacehub.security.EmailValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserAccountService {

  private final VerificationService verificationService;
  private final EmailValidator emailValidator;
  private final OTPService otpService;
  private final UserService userService;
  private final RefreshTokenService refreshTokenService;
  private final PasswordEncoder passwordEncoder;

  public UserAccountService(VerificationService verificationService,
                            EmailValidator emailValidator,
                            OTPService otpService,
                            UserService userService,
                            RefreshTokenService refreshTokenService,
                            PasswordEncoder passwordEncoder) {
    this.verificationService = verificationService;
    this.emailValidator = emailValidator;
    this.otpService = otpService;
    this.userService = userService;
    this.refreshTokenService = refreshTokenService;
    this.passwordEncoder = passwordEncoder;
  }

  public ApiResponse<TokenResponse> login(LoginRequest request) {
    String email = emailValidator.normalize(request.getEmail());
    User user;
    try {
      user = userService.getUserByEmail(email);
    } catch (Exception e) {
      return new ApiResponse<>(400, "User not found", null);
    }
    if (!Boolean.TRUE.equals(user.getEnabled())) {
      return new ApiResponse<>(400, "Account not enabled. " +
        "Please verify your email/OTP first.", null);
    }
    if (!verificationService.checkCredentials(user.getEmail(), request.getPassword())) {
      return new ApiResponse<>(400, "Invalid credentials", null);
    }
    if (otpService.isInCooldown(email, OtpType.LOGIN)) {
      long secondsLeft = otpService.cooldownTime(email, OtpType.LOGIN);
      return new ApiResponse<>(400,
        "Please wait " + secondsLeft + " seconds before requesting another login OTP.",
        null);
    }
    otpService.sendOTP(email, OtpType.LOGIN);
    return new ApiResponse<>(200, "OTP sent for login verification", null);
  }

  public ApiResponse<String> register(RegistrationRequest request) {
    String email = emailValidator.normalize(request.getEmail());

    if (userService.existsByEmail(email)) {
      return new ApiResponse<>(400, "User already exists", null);
    }

    if (otpService.isInCooldown(email, OtpType.REGISTRATION)) {
      long secondsLeft = otpService.cooldownTime(email, OtpType.REGISTRATION);
      return new ApiResponse<>(400,
        "Please wait " + secondsLeft + " seconds before requesting OTP again.", null);
    }

    try {
      request.setEmail(email);
      otpService.saveTempOtp(email, request);
      otpService.sendOTP(email, OtpType.REGISTRATION);
      return new ApiResponse<>(201, "OTP sent. Complete registration by validating OTP.",
        null);
    } catch (Exception e) {
      return new ApiResponse<>(500, "Registration failed", null);
    }
  }

  public ApiResponse<?> validateOTP(OTPRequest request) {
    String email = emailValidator.normalize(request.getEmail());
    OtpType type = request.getType();

    if (otpService.isUsed(email, type)) {
      return new ApiResponse<>(400, "OTP has already been used", null);
    }

    boolean valid = otpService.validateOTP(email, request.getOtp(), type);
    if (!valid) {
      return new ApiResponse<>(400, "OTP is invalid or expired", null);
    }

    otpService.markAsUsed(email, request.getOtp(), type);

    return switch (type) {
      case REGISTRATION -> handleRegistrationOTP(email);
      case LOGIN -> handleLoginOTP(email);
      case FORGOT_PASSWORD -> handleForgotPasswordOTP(email);
    };
  }



  public ApiResponse<String> forgotPassword(String email) {
    String normalizedEmail = emailValidator.normalize(email);

    try {
      userService.getUserByEmail(normalizedEmail);
    } catch (Exception e) {
      return new ApiResponse<>(400, "User not found", null);
    }

    if (otpService.isInCooldown(normalizedEmail, OtpType.FORGOT_PASSWORD)) {
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

  private ApiResponse<?> handleRegistrationOTP(String email) {
    if (userService.existsByEmail(email)) {
      return new ApiResponse<>(400, "User already registered", null);
    }

    RegistrationRequest tempRequest = otpService.getTempOtp(email);
    if (tempRequest == null) {
      return new ApiResponse<>(400, "Registration session expired", null);
    }

    User newUser = new User();
    newUser.setFirstName(tempRequest.getFirstName());
    newUser.setLastName(tempRequest.getLastName());
    newUser.setEmail(email);
    newUser.setPassword(passwordEncoder.encode(tempRequest.getPassword()));
    newUser.setIsVerifiedRegistration(true);
    newUser.setEnabled(true);
    newUser.setLocked(false);
    newUser.setUserRole(UserRole.USER);
    userService.save(newUser);
    otpService.deleteTempOtp(email);

    return new ApiResponse<>(200, "Registration verified successfully", null);
  }

  private ApiResponse<?> handleLoginOTP(String email) {
    User loginUser;
    try {
      loginUser = userService.getUserByEmail(email);
    } catch (Exception e) {
      return new ApiResponse<>(400, "User not found", null);
    }

    if (loginUser.getIsVerifiedLogin()) {
      return new ApiResponse<>(400, "User is already logged in", null);
    }

    loginUser.setIsVerifiedLogin(true);
    userService.save(loginUser);

    TokenResponse tokens = verificationService.generateTokens(loginUser);
    return new ApiResponse<>(200, "Login verified successfully", tokens);
  }

  private ApiResponse<?> handleForgotPasswordOTP(String email) {
    User forgotUser;
    try {
      forgotUser = userService.getUserByEmail(email);
    } catch (Exception e) {
      return new ApiResponse<>(400, "User not found", null);
    }

    if (forgotUser.getIsVerifiedForgot()) {
      return new ApiResponse<>(400, "Password reset already verified", null);
    }

    forgotUser.setIsVerifiedForgot(true);
    userService.save(forgotUser);

    return new ApiResponse<>(200, "OTP verified. You can reset your password now.", null);
  }
}

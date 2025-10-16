package org.spacehub.service;

import org.spacehub.DTO.*;
import org.spacehub.entities.ApiResponse;
import org.spacehub.entities.User;
import org.spacehub.entities.RegistrationRequest;
import org.spacehub.entities.OtpType;
import org.spacehub.entities.UserRole;
import org.spacehub.security.EmailValidator;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserAccountService {

  private final VerificationService verificationService;
  private final EmailValidator emailValidator;
  private final OTPService otpService;
  private final UserService userService;
  private final RefreshTokenService refreshTokenService;
  private final PasswordEncoder passwordEncoder;
  private final RedisService redisService;
  private final UserNameService userNameService;

  private static final int TEMP_TOKEN_EXPIRE = 300;
  private static final int OTP_EXPIRE = 300;

  public UserAccountService(VerificationService verificationService,
                            EmailValidator emailValidator,
                            OTPService otpService,
                            UserService userService,
                            RefreshTokenService refreshTokenService,
                            PasswordEncoder passwordEncoder,
                            RedisService redisService,
                            UserNameService userNameService) {
    this.verificationService = verificationService;
    this.emailValidator = emailValidator;
    this.otpService = otpService;
    this.userService = userService;
    this.refreshTokenService = refreshTokenService;
    this.passwordEncoder = passwordEncoder;
    this.redisService = redisService;
    this.userNameService = userNameService;
  }

  public ApiResponse<TokenResponse> login(LoginRequest request) {

    if (request == null || request.getEmail() == null || request.getPassword() == null) {
      return new ApiResponse<>(400, "Email and password are required", null);
    }

    String email = emailValidator.normalize(request.getEmail());
    User user;
    try {
      user = userService.getUserByEmail(email);
    }
    catch (UsernameNotFoundException e) {
      return new ApiResponse<>(404, "User not found", null);
    }
    catch (Exception e) {
      return new ApiResponse<>(400, "User not found", null);
    }
    if (!Boolean.TRUE.equals(user.getEnabled())) {
      return new ApiResponse<>(403, "Account not enabled. " +
        "Please verify your email/OTP first.", null);
    }
    if (!verificationService.checkCredentials(user.getEmail(), request.getPassword())) {
      return new ApiResponse<>(401, "Invalid credentials", null);
    }
    try {
      TokenResponse tokens = verificationService.generateTokens(user);
      return new ApiResponse<>(200, "Logged in successfully", tokens);
    }
    catch (Exception e) {
      return new ApiResponse<>(500, "Failed to generate tokens", null);
    }
  }

  public ApiResponse<String> register(RegistrationRequest request) {

    if (request == null) {
      return new ApiResponse<>(400, "Registration data is required", null);
    }

    String email = emailValidator.normalize(request.getEmail());

    if (email == null || email.isBlank()) {
      return new ApiResponse<>(400, "Invalid email", null);
    }

    if (userService.existsByEmail(email)) {
      return new ApiResponse<>(400, "User already exists", null);
    }

    if (otpService.isInCooldown(email, OtpType.REGISTRATION)) {
      long secondsLeft = otpService.cooldownTime(email, OtpType.REGISTRATION);
      return new ApiResponse<>(400,
        "Please wait " + secondsLeft + " seconds before requesting OTP again.", null);
    }

    try {
      RegistrationRequest tempRegistration = new RegistrationRequest();
      tempRegistration.setFirstName(request.getFirstName());
      tempRegistration.setLastName(request.getLastName());
      tempRegistration.setEmail(email);
      tempRegistration.setPassword(passwordEncoder.encode(request.getPassword()));

      otpService.saveTempOtp(email, tempRegistration);

      otpService.sendOTP(email, OtpType.REGISTRATION);

      return new ApiResponse<>(201, "OTP sent. Complete registration by validating OTP.", null);
    }
    catch (RuntimeException e) {
      return new ApiResponse<>(500, "Registration failed. Please try again later.", null);
    }
  }

  public ApiResponse<?> validateOTP(OTPRequest request) {

    if (request.getEmail() == null || request.getOtp() == null || request.getType() == null) {
      return new ApiResponse<>(400, "Email, OTP, and OTP type are required.", null);
    }

    String email = emailValidator.normalize(request.getEmail());
    OtpType type = request.getType();

    if (type != OtpType.REGISTRATION) {
      return new ApiResponse<>(400, "Only registration OTP can be validated here.", null);
    }

    if (otpService.isBlocked(email, type)) {
      return new ApiResponse<>(429, "Too many invalid OTP attempts. Try again later.", null);
    }

    if (otpService.isUsed(email, type)) {
      return new ApiResponse<>(400, "OTP has already been used", null);
    }

    boolean valid = otpService.validateOTP(email, request.getOtp(), type);
    if (!valid) {
      long attempts = otpService.incrementOtpAttempts(email, type);
      if (attempts >= 3) {
        otpService.blockOtp(email, type);
        return new ApiResponse<>(429, "Too many invalid attempts. Please request a new OTP.", null);
      }

      return new ApiResponse<>(400, "Invalid or expired OTP. Attempts left: " + (3 - attempts), null);
    }

    otpService.markAsUsed(email, request.getOtp(), type);

    return handleRegistrationOTP(email);
  }

  public ApiResponse<String> forgotPassword(String email) {
    String normalizedEmail = emailValidator.normalize(email);

    User user;
    try {
      user = userService.getUserByEmail(normalizedEmail);
    }
    catch (Exception e) {
      return new ApiResponse<>(200, "If this email is registered, an OTP has been sent.", null);
    }

    if (otpService.isInCooldown(normalizedEmail, OtpType.FORGOT_PASSWORD)) {
      long secondsLeft = otpService.cooldownTime(normalizedEmail, OtpType.FORGOT_PASSWORD);
      return new ApiResponse<>(400,
              "Please wait " + secondsLeft + " seconds before requesting OTP again.", null);
    }

    String tempToken = otpService.sendOTPWithTempToken(user, OtpType.FORGOT_PASSWORD);
    return new ApiResponse<>(200, "OTP sent to your email", tempToken);
  }

  public ApiResponse<String> resetPassword(ResetPasswordRequest request) {
    String email = emailValidator.normalize(request.getEmail());
    String tempToken = request.getTempToken();
    String newPassword = request.getNewPassword();

    String savedToken = redisService.getValue("TEMP_RESET_" + email);
    if (savedToken == null || !savedToken.equals(tempToken)) {
      return new ApiResponse<>(401, "Unauthorized. OTP not validated or token expired.", null);
    }

    User user;
    try {
      user = userService.getUserByEmail(email);
    } catch (Exception e) {
      return new ApiResponse<>(400, "User not found", null);
    }

    userService.updatePassword(email, passwordEncoder.encode(newPassword));
    redisService.deleteValue("TEMP_RESET_" + email);

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
    try {
      RegistrationRequest tempRequest = otpService.getTempOtp(email);
      if (tempRequest == null) {
        return new ApiResponse<>(400, "Registration session expired or not found", null);
      }

      User existingUser = null;
      try {
        existingUser = userService.getUserByEmail(email);
      }
      catch (Exception ignored) {
      }

      if (existingUser != null) {
        return new ApiResponse<>(400, "User already registered", null);
      }

      User newUser = new User();
      newUser.setFirstName(tempRequest.getFirstName());
      newUser.setLastName(tempRequest.getLastName());
      newUser.setEmail(email);
      newUser.setPassword(tempRequest.getPassword());
      newUser.setIsVerifiedRegistration(true);
      newUser.setEnabled(true);
      newUser.setLocked(false);
      newUser.setUserRole(UserRole.USER);

      userService.save(newUser);

      otpService.deleteTempOtp(email);
      otpService.deleteOTP(email, OtpType.REGISTRATION);

      return new ApiResponse<>(200, "Registration verified successfully", null);

    } catch (Exception e) {
      return new ApiResponse<>(500, "Registration verification failed: " + e.getMessage(), null);
    }
  }

  public ApiResponse<String> resendOTP(String email) {
    String normalizedEmail = emailValidator.normalize(email);

    User existingUser = null;
    try {
      existingUser = userService.getUserByEmail(normalizedEmail);
      if (existingUser != null && Boolean.TRUE.equals(existingUser.getEnabled())) {
        return new ApiResponse<>(400, "User already verified. No OTP needed.", null);
      }
    } catch (Exception ignored) {
    }

    if (otpService.isInCooldown(normalizedEmail, OtpType.REGISTRATION)) {
      long secondsLeft = otpService.cooldownTime(normalizedEmail, OtpType.REGISTRATION);
      return new ApiResponse<>(400,
              "Please wait " + secondsLeft + " seconds before requesting OTP again.", null);
    }

    otpService.sendOTP(normalizedEmail, OtpType.REGISTRATION);
    return new ApiResponse<>(200, "OTP resent successfully. Check your email.", null);
  }

  public ApiResponse<String> validateForgotPasswordOtp(ValidateForgotOtpRequest request) {
    String email = emailValidator.normalize(request.getEmail());
    String otp = request.getOtp();

    if (otpService.isBlocked(email, OtpType.FORGOT_PASSWORD)) {
      return new ApiResponse<>(429, "Too many invalid OTP attempts. Try again later.", null);
    }

    if (otpService.isUsed(email, OtpType.FORGOT_PASSWORD)) {
      return new ApiResponse<>(400, "OTP has already been used", null);
    }

    boolean valid = otpService.validateOTP(email, otp, OtpType.FORGOT_PASSWORD);
    if (!valid) {
      long attempts = otpService.incrementOtpAttempts(email, OtpType.FORGOT_PASSWORD);
      if (attempts >= 3) {
        otpService.blockOtp(email, OtpType.FORGOT_PASSWORD);
        return new ApiResponse<>(429, "Too many invalid OTP attempts. Request a new OTP.", null);
      }
      return new ApiResponse<>(400, "Invalid or expired OTP. Attempts left: " + (3 - attempts), null);
    }

    otpService.markAsUsed(email, otp, OtpType.FORGOT_PASSWORD);

    User user = userService.getUserByEmail(email);
    String tempToken = userNameService.generateToken(user);
    redisService.saveValue("TEMP_RESET_" + email, tempToken, TEMP_TOKEN_EXPIRE);

    return new ApiResponse<>(200, "OTP validated successfully", tempToken);
  }

  public ApiResponse<String> resendForgotPasswordOtp(String tempToken) {
    String email = otpService.extractEmailFromToken(tempToken, OtpType.FORGOT_PASSWORD);

    if (email == null) {
      return new ApiResponse<>(403, "Session expired or invalid", null);
    }

    if (otpService.isInCooldown(email, OtpType.FORGOT_PASSWORD)) {
      long secondsLeft = otpService.cooldownTime(email, OtpType.FORGOT_PASSWORD);
      return new ApiResponse<>(400, "Please wait " + secondsLeft + " seconds before requesting OTP again.", null);
    }

    User user;
    try {
      user = userService.getUserByEmail(email);
    } catch (Exception e) {
      return new ApiResponse<>(404, "User not found", null);
    }

    String newTempToken = otpService.sendOTPWithTempToken(user, OtpType.FORGOT_PASSWORD);

    otpService.deleteTempToken(email, OtpType.FORGOT_PASSWORD, tempToken);

    return new ApiResponse<>(200, "OTP resent successfully. Check your email.", newTempToken);
  }


}

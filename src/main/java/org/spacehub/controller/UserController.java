package org.spacehub.controller;

import org.spacehub.DTO.*;
import org.spacehub.entities.ApiResponse;
import org.spacehub.entities.User;
import org.spacehub.entities.RegistrationRequest;
import org.spacehub.entities.OtpType;
import org.spacehub.security.EmailValidator;
import org.spacehub.service.OTPService;
import org.spacehub.service.RefreshTokenService;
import org.spacehub.service.UserNameService;
import org.spacehub.service.UserService;
import org.spacehub.service.VerificationService;
import org.spacehub.service.RegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "api/v1")
public class UserController {

  private final VerificationService verificationService;
  private final RegistrationService registrationService;
  private final EmailValidator emailValidator;
  private final OTPService otpService;
  private final UserService userService;
  private final UserNameService userNameService;
  private final RefreshTokenService refreshTokenService;

  public UserController(VerificationService verificationService,
                        RegistrationService registrationService, EmailValidator emailValidator,
                        OTPService otpService, UserService userService,
                        UserNameService userNameService,
                        RefreshTokenService refreshTokenService) {
    this.verificationService = verificationService;
    this.registrationService = registrationService;
    this.emailValidator = emailValidator;
    this.otpService = otpService;
    this.userService = userService;
    this.userNameService = userNameService;
    this.refreshTokenService = refreshTokenService;
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<TokenResponse>> login(@RequestBody LoginRequest request) {
    String email = emailValidator.normalize(request.getEmail());
    if (!emailValidator.test(email)) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(400,
        "Invalid email format!", null));
    }

    if (!otpService.canSendOTP(email, OtpType.LOGIN)) {
      long secondsLeft = otpService.cooldownTime(email, OtpType.LOGIN);
      return ResponseEntity.badRequest().body(new ApiResponse<>(400,
              "Please wait " + secondsLeft + " seconds before trying to login again.",
        null));
    }

    User user;
    try {
      user = userService.getUserByEmail(email);
    }
    catch (Exception e) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(400,
        "User not found", null));
    }

    if (!verificationService.checkCredentials(user.getEmail(), request.getPassword())) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(400,
        "Invalid credentials", null));
    }

    otpService.sendOTP(email, OtpType.LOGIN);
    return ResponseEntity.ok(new ApiResponse<>(200,
      "OTP sent for login verification", null));
  }

  @PostMapping("/registration")
  public ResponseEntity<ApiResponse<String>> register(@RequestBody RegistrationRequest request) {
    String email = emailValidator.normalize(request.getEmail());
    if (!emailValidator.test(email)) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(400,
        "Invalid email format!", null));
    }

    if (!otpService.canSendOTP(email, OtpType.REGISTRATION)) {
      long secondsLeft = otpService.cooldownTime(email, OtpType.REGISTRATION);
      return ResponseEntity.badRequest().body(new ApiResponse<>(400,
              "Please wait " + secondsLeft +
                " seconds before registering again.", null));
    }

    request.setEmail(email);
    try {
      User user = registrationService.register(request);
      user.setIsVerifiedRegistration(false);
      userService.save(user);

      otpService.sendOTP(email, OtpType.REGISTRATION);
      return ResponseEntity.status(201).body(new ApiResponse<>(201,
        "OTP sent. Complete registration by validating OTP.", null));
    }
    catch (IllegalStateException e) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(400, e.getMessage(),
        null));
    }
    catch (Exception e) {
      System.out.println(e.getMessage());
      return ResponseEntity.internalServerError().body(new ApiResponse<>(500,
        "Registration failed", null));
    }
  }

  @PostMapping("/sendotp")
  public ResponseEntity<ApiResponse<String>> sendOTP(@RequestBody OTPRequest request) {
    String email = emailValidator.normalize(request.getEmail());
    OtpType type = request.getType();

    if (email == null || !emailValidator.test(email) || type == null) {
      return ResponseEntity.badRequest()
              .body(new ApiResponse<>(400, "Invalid email or missing OTP type",
                null));
    }

    if (!otpService.canSendOTP(email, type)) {
      long secondsLeft = otpService.cooldownTime(email, type);
      return ResponseEntity.badRequest()
              .body(new ApiResponse<>(400,
                      "Please wait " + secondsLeft + " seconds before requesting OTP again.", null));
    }

    otpService.sendOTP(email, type);
    return ResponseEntity.ok(new ApiResponse<>(200,
            "OTP sent successfully to " + email + " for " + type, null));
  }

  @PostMapping("/validateotp")
  public ResponseEntity<ApiResponse<?>> validateOTP(@RequestBody OTPRequest request) {
    OtpType type = request.getType();
    boolean valid = otpService.validateOTP(request.getEmail(), request.getOtp(), type);
    if (!valid) {
      return ResponseEntity.status(400).body(new ApiResponse<>(400,
        "OTP is invalid or expired", null));
    }

    User user;
    try {
      user = userService.getUserByEmail(request.getEmail());
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(400, "User not found",
        null));
    }
    if (type == OtpType.REGISTRATION) {
      user.setIsVerifiedRegistration(true);
      userService.save(user);
      return ResponseEntity.ok(new ApiResponse<>(200,
        "Registration verified successfully", null));
    }
    else if (type == OtpType.LOGIN) {
      user.setIsVerifiedLogin(true);
      userService.save(user);
      TokenResponse tokens = verificationService.generateTokens(user);
      return ResponseEntity.ok(new ApiResponse<>(200,
        "Login verified successfully", tokens));
    }
    else if (type == OtpType.FORGOT_PASSWORD) {
      user.setIsVerifiedForgot(true);
      userService.save(user);
      return ResponseEntity.ok(new ApiResponse<>(200,
        "OTP verified. You can reset your password now.", null));
    }

    return ResponseEntity.ok(new ApiResponse<>(200, "OTP verified", null));
  }

  @PostMapping("/forgotpassword")
  public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody EmailRequest request) {
    String email = emailValidator.normalize(request.getEmail());
    if (!emailValidator.test(email)) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(400,
        "Invalid email format!", null));
    }
    User user;
    try {
      user = userService.getUserByEmail(email);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(400,
        "User not found", null));
    }

    if (!otpService.canSendOTP(email, OtpType.FORGOT_PASSWORD)) {
      long secondsLeft = otpService.cooldownTime(email, OtpType.FORGOT_PASSWORD);
      return ResponseEntity.badRequest().body(new ApiResponse<>(400,
              "Please wait " + secondsLeft + " seconds before requesting OTP again.",
        null));
    }

    otpService.sendOTP(email, OtpType.FORGOT_PASSWORD);
    return ResponseEntity.ok(new ApiResponse<>(200, "OTP sent to your email",
      null));
  }

  @PostMapping("/resetpassword")
  public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody ResetPasswordRequest request) {
    String email = emailValidator.normalize(request.getEmail());
    String newPassword = request.getNewPassword();

    User user;
    try {
      user = userService.getUserByEmail(email);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(400, "User not found", null));
    }

    if (!user.getIsVerifiedForgot()) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(400, "OTP not verified for password reset", null));
    }

    userService.updatePassword(email, newPassword);
    user.setIsVerifiedForgot(false);
    userService.save(user);

    return ResponseEntity.ok(new ApiResponse<>(200, "Password has been reset successfully", null));
  }


  @PostMapping("/token/refresh")
  public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
    @RequestBody RefreshRequest request) {
    String incomingToken = request.getRefreshToken();
    if (incomingToken == null || incomingToken.isBlank()) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(400,
        "Missing refresh token", null));
    }
    var opt = refreshTokenService.findByToken(incomingToken);
    if (opt.isEmpty()) {
      return ResponseEntity.status(401).body(new ApiResponse<>(401,
        "Invalid refresh token", null));
    }

    var refreshToken = opt.get();
    if (refreshTokenService.isExpired(refreshToken)) {
      refreshTokenService.deleteByToken(incomingToken);
      return ResponseEntity.status(401).body(new ApiResponse<>(401,
        "Refresh token expired", null));
    }

    User user = refreshToken.getUser();
    refreshTokenService.deleteByToken(incomingToken);
    var newRefresh = refreshTokenService.createRefreshToken(user);

    UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
    String newAccessToken = userNameService.generateToken(userDetails);

    TokenResponse tokens = new TokenResponse(newAccessToken, newRefresh.getToken());
    return ResponseEntity.ok(new ApiResponse<>(200, "Token refreshed", tokens));
  }
}

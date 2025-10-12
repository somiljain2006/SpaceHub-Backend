package org.spacehub.controller;

import org.spacehub.DTO.EmailRequest;
import org.spacehub.DTO.LoginRequest;
import org.spacehub.DTO.OTPRequest;
import org.spacehub.DTO.RefreshRequest;
import org.spacehub.DTO.TokenResponse;
import org.spacehub.entities.ApiResponse;
import org.spacehub.entities.User;
import org.spacehub.entities.RegistrationRequest;
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

    User user = new User();
    user.setEmail(email);
    user.setPassword(request.getPassword());

    TokenResponse tokens = verificationService.authenticateAndIssueTokens(user);
    if (tokens == null) {
      return ResponseEntity.status(400).body(new ApiResponse<>(400,
        "Invalid credentials", null));
    }
    return ResponseEntity.ok(new ApiResponse<>(201, "Login successful", tokens));
  }

  @PostMapping("/registration")
  public ResponseEntity<ApiResponse<String>> register(@RequestBody RegistrationRequest request) {
    String email = emailValidator.normalize(request.getEmail());
    if (!emailValidator.test(email)) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(400,
        "Invalid email format!", null));
    }
    request.setEmail(email);
    try {
      String token = registrationService.register(request);
      return ResponseEntity.status(201).body(new ApiResponse<>(201,
        "Registration successful", token));
    } catch (IllegalStateException e) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(400, e.getMessage(),
        null));
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body(new ApiResponse<>(500,
        "Registration failed", null));
    }
  }

  @PostMapping("/sendotp")
  public ResponseEntity<ApiResponse<String>> sendOTP(@RequestBody EmailRequest request) {
    String email = emailValidator.normalize(request.getEmail());
    if (email == null || !emailValidator.test(email)) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(400,
        "Invalid or missing email!", null));
    }
    if (!otpService.canSendOTP(email)) {
      long secondsLeft = otpService.cooldownTime(email);
      return ResponseEntity.badRequest().body(new ApiResponse<>(400,
        "Please wait " + secondsLeft + " seconds before requesting OTP again.", null));
    }
    otpService.sendOTP(email);
    return ResponseEntity.ok(new ApiResponse<>(200,
      "OTP sent successfully to " + email, null));
  }

  @PostMapping("/validateotp")
  public ResponseEntity<ApiResponse<String>> validateOTP(@RequestBody OTPRequest request) {
    boolean valid = otpService.validateOTP(request.getEmail(), request.getOtp());
    if (valid) {
      return ResponseEntity.ok(new ApiResponse<>(200, "OTP is valid", null));
    } else {
      return ResponseEntity.status(400).body(new ApiResponse<>(400,
        "OTP is invalid or expired", null));
    }
  }

  @PostMapping("/forgotpassword")
  public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody EmailRequest request) {
    String email = emailValidator.normalize(request.getEmail());
    if (email == null || !emailValidator.test(email)) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(400,
        "Invalid email format!", null));
    }
    if (!userService.checkUser(email)) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(400,
        "User with this email does not exist", null));
    }
    otpService.sendOTP(email);
    return ResponseEntity.ok(new ApiResponse<>(200, "OTP sent to your email",
      null));
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

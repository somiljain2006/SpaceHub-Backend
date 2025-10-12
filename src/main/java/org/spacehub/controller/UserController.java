package org.spacehub.controller;

import org.spacehub.DTO.EmailRequest;
import org.spacehub.DTO.LoginRequest;
import org.spacehub.DTO.OTPRequest;
import org.spacehub.entities.ApiResponse;
import org.spacehub.entities.User;
import org.spacehub.entities.RegistrationRequest;
import org.spacehub.security.EmailValidator;
import org.spacehub.service.OTPService;
import org.spacehub.service.UserService;
import org.spacehub.service.VerificationService;
import org.spacehub.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

  @Autowired
  private OTPService otpService;

  @Autowired
  private UserService userService;

  public UserController(VerificationService verificationService, RegistrationService registrationService, EmailValidator emailValidator) {
    this.verificationService = verificationService;
    this.registrationService = registrationService;
    this.emailValidator = emailValidator;
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<String>> login(@RequestBody LoginRequest request) {
    String email = emailValidator.normalize(request.getEmail());
    if (!emailValidator.test(email)) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Invalid email format!", null));
    }

    User user = new User();
    user.setEmail(email);
    user.setPassword(request.getPassword());

    String token = verificationService.check(user);
    if (token == null) {
      return ResponseEntity.status(401).body(new ApiResponse<>(401, "Invalid credentials", null));
    }
    return ResponseEntity.ok(new ApiResponse<>(200, "Login successful", token));
  }

  @PostMapping("/registration")
  public ResponseEntity<ApiResponse<String>> register(@RequestBody RegistrationRequest request) {
    String email = emailValidator.normalize(request.getEmail());
    if (!emailValidator.test(email)) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Invalid email format!", null));
    }
    request.setEmail(email);
    try {
      String token = registrationService.register(request);
      return ResponseEntity.status(201).body(new ApiResponse<>(201, "Registration successful", token));
    } catch (IllegalStateException e) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(400, e.getMessage(), null));
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body(new ApiResponse<>(500, "Registration failed", null));
    }
  }

  @PostMapping("/sendotp")
  public ResponseEntity<ApiResponse<String>> sendOTP(@RequestBody EmailRequest request) {
    String email = emailValidator.normalize(request.getEmail());
    if (email == null || !emailValidator.test(email)) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Invalid or missing email!", null));
    }
    if (!otpService.canSendOTP(email)) {
      long secondsLeft = otpService.cooldownTime(email);
      return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Please wait " + secondsLeft + " seconds before requesting OTP again.", null));
    }
    otpService.sendOTP(email);
    return ResponseEntity.ok(new ApiResponse<>(200, "OTP sent successfully to " + email, null));
  }

  @PostMapping("/validateotp")
  public ResponseEntity<ApiResponse<String>> validateOTP(@RequestBody OTPRequest request) {
    boolean valid = otpService.validateOTP(request.getEmail(), request.getOtp());
    if (valid) {
      return ResponseEntity.ok(new ApiResponse<>(200, "OTP is valid", null));
    } else {
      return ResponseEntity.status(400).body(new ApiResponse<>(400, "OTP is invalid or expired", null));
    }
  }

  @PostMapping("/forgotpassword")
  public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody EmailRequest request) {
    String email = emailValidator.normalize(request.getEmail());
    if (email == null || !emailValidator.test(email)) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(400, "Invalid email format!", null));
    }
    if (!userService.checkUser(email)) {
      return ResponseEntity.badRequest().body(new ApiResponse<>(400, "User with this email does not exist", null));
    }
    otpService.sendOTP(email);
    return ResponseEntity.ok(new ApiResponse<>(200, "OTP sent to your email", null));
  }


}

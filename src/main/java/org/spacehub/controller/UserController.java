package org.spacehub.controller;

import org.spacehub.entities.User;
import org.spacehub.entities.RegistrationRequest;
import org.spacehub.service.OTPService;
import org.spacehub.service.VerificationService;
import org.spacehub.service.RegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "api/v1")
public class UserController {

  private final VerificationService verificationService;
  private final RegistrationService registrationService;

  @Autowired
  private OTPService otpService;

  public UserController(VerificationService verificationService, RegistrationService registrationService) {
    this.verificationService = verificationService;
    this.registrationService = registrationService;
  }

  @PostMapping("/login")
  public ResponseEntity<String> login(@RequestBody User user) {
    String token = verificationService.check(user);
    if (token == null) return ResponseEntity.status(401).body("Invalid credentials");
    return ResponseEntity.ok(token);
  }

  @PostMapping("/registration")
  public ResponseEntity<String> register(@RequestBody RegistrationRequest request) {
    try {
      String result = registrationService.register(request);
      return ResponseEntity.status(201).body(result);
    } catch (IllegalStateException ex) {
      return ResponseEntity.badRequest().body(ex.getMessage());
    } catch (Exception ex) {
      return ResponseEntity.internalServerError().body("Registration failed");
    }
  }

  @GetMapping("/sendotp")
  public ResponseEntity<String> sendOTP(@RequestParam(required = false) String email) {
    try {
      otpService.sendOTP(email);
      return ResponseEntity.ok("OTP sent successfully to " + (email != null ? email : "default email"));
    } catch (Exception e) {
      return ResponseEntity.status(500).body("Error sending OTP: " + e.getMessage());
    }
  }

  @GetMapping("/validateotp")
  public ResponseEntity<String> validateOTP(@RequestParam String otp) {
    boolean valid = otpService.validateOTP(otp);
    if (valid) {
      return ResponseEntity.ok("OTP is valid");
    } else {
      return ResponseEntity.status(400).body("OTP is invalid or expired");
    }
  }
}

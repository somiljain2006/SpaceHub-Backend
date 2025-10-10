package org.spacehub.controller;

import org.spacehub.entities.User;
import org.spacehub.service.VerificationService;
import org.spacehub.entities.RegistrationRequest;
import org.spacehub.service.RegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "api/v1/registration")
public class UserController {

  private final VerificationService verificationService;
  private final RegistrationService registrationService;

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

  @PostMapping
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
}

package org.spacehub.controller;

import lombok.AllArgsConstructor;
import org.spacehub.entities.RegistrationRequest;
import org.spacehub.service.RegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "api/v1/registration")
@AllArgsConstructor
public class RegistrationController {

  private final RegistrationService registrationService;

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

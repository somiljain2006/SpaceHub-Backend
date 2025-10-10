package org.spacehub.controller;

import org.spacehub.entity.User;
import org.spacehub.service.VerificationService;
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
public class UserController {

    VerificationService verificationService;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        String token = verificationService.check(user);

        return ResponseEntity.ok(token);
    }

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

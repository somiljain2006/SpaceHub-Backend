package org.spacehub.controller;

import org.spacehub.entity.User;
import org.spacehub.service.VerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    VerificationService verificationService;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        String token = verificationService.check(user);

        return ResponseEntity.ok(token);
    }

}

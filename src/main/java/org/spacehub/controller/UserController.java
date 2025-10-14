package org.spacehub.controller;

import jakarta.validation.Valid;
import org.spacehub.DTO.*;
import org.spacehub.entities.ApiResponse;
import org.spacehub.entities.RegistrationRequest;
import org.spacehub.service.UserAccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "api/v1")
public class UserController {

  private final UserAccountService accountService;

  public UserController(UserAccountService accountService) {
    this.accountService = accountService;
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<TokenResponse>> login(@RequestBody LoginRequest request) {
    ApiResponse<TokenResponse> resp = accountService.login(request);
    return ResponseEntity.status(resp.getStatus()).body(resp);
  }

  @PostMapping("/registration")
  public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegistrationRequest request) {
    ApiResponse<String> resp = accountService.register(request);
    return ResponseEntity.status(resp.getStatus()).body(resp);
  }

  @PostMapping("/validateotp")
  public ResponseEntity<ApiResponse<?>> validateOTP(@RequestBody OTPRequest request) {
    ApiResponse<?> resp = accountService.validateOTP(request);
    return ResponseEntity.status(resp.getStatus()).body(resp);
  }

  @GetMapping("/forgotpassword")
  public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestParam String email) {
    ApiResponse<String> resp = accountService.forgotPassword(email);
    return ResponseEntity.status(resp.getStatus()).body(resp);
  }

  @PostMapping("/resetpassword")
  public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody ResetPasswordRequest request) {
    ApiResponse<String> resp = accountService.resetPassword(request);
    return ResponseEntity.status(resp.getStatus()).body(resp);
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<String>> logout(@RequestBody(required = false) RefreshRequest request) {
    ApiResponse<String> resp = accountService.logout(request);
    return ResponseEntity.status(resp.getStatus()).body(resp);
  }
}

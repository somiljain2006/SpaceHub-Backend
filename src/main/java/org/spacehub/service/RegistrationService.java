package org.spacehub.service;

import jakarta.transaction.Transactional;
import org.spacehub.entities.RegistrationRequest;
import org.spacehub.entities.User;
import org.spacehub.entities.UserRole;
import org.spacehub.repository.UserRepository;
import org.spacehub.security.ConfirmationToken;
import org.spacehub.security.EmailValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RegistrationService {

  private final PasswordEncoder passwordEncoder;
  private final EmailValidator emailValidator;
  private final UserRepository userRepository;
  private final ConfirmationTokenService confirmationTokenService;
  private final OTPService otpService;

  public RegistrationService(PasswordEncoder passwordEncoder, EmailValidator emailValidator,
                             UserRepository userRepository, ConfirmationTokenService confirmationTokenService,
                             OTPService otpService) {
    this.passwordEncoder = passwordEncoder;
    this.emailValidator = emailValidator;
    this.userRepository = userRepository;
    this.confirmationTokenService = confirmationTokenService;
    this.otpService = otpService;
  }

  @Transactional
  public String register(RegistrationRequest request) {
    String email = request.getEmail();
    if (!emailValidator.test(email)) {
      throw new IllegalStateException("Invalid email address");
    }

    if (userRepository.findByEmail(email).isPresent()) {
      throw new IllegalStateException("Email already taken");
    }

    User newUser = new User(
            request.getFirstName(),
            request.getLastName(),
            email,
            passwordEncoder.encode(request.getPassword()),
            UserRole.USER
    );
    newUser.setEnabled(true);
    userRepository.save(newUser);

    String token = UUID.randomUUID().toString();
    ConfirmationToken confirmationToken = new ConfirmationToken(
            token,
            LocalDateTime.now(),
            LocalDateTime.now().plusMinutes(15),
            newUser
    );
    confirmationTokenService.saveConfirmationToken(confirmationToken);

    otpService.sendOTP(newUser.getEmail());
    return token;
  }
}

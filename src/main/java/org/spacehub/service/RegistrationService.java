package org.spacehub.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.spacehub.entities.RegistrationRequest;
import org.spacehub.entities.User;
import org.spacehub.entities.UserRole;
import org.spacehub.repository.UserRepository;
import org.spacehub.security.ConfirmationToken;
import org.spacehub.security.EmailValidator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
public class RegistrationService {

  private final EmailValidator emailValidator;
  private final BCryptPasswordEncoder bCryptPasswordEncoder;
  private final UserRepository userRepository;
  private final ConfirmationTokenService confirmationTokenService;

  @Transactional
  public String register(RegistrationRequest request) {
    boolean isValidEmail = emailValidator.test(request.email());
    if (!isValidEmail) {
      throw new IllegalStateException("Invalid email address");
    }
    if (userRepository.findByEmail(request.email()).isPresent()) {
      throw new IllegalStateException("Email already taken");
    }
    User newUser = new User(
      request.firstName(),
      request.lastName(),
      request.email(),
      bCryptPasswordEncoder.encode(request.password()),
      UserRole.USER
    );
    userRepository.save(newUser);
    String token = UUID.randomUUID().toString();
    ConfirmationToken confirmationToken = new ConfirmationToken(
      token,
      LocalDateTime.now(),
      LocalDateTime.now().plusMinutes(15),
      newUser
    );
    confirmationTokenService.saveConfirmationToken(confirmationToken);
    return token;
  }
}

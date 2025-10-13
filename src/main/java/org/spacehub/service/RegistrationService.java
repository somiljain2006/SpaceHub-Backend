package org.spacehub.service;

import jakarta.transaction.Transactional;
import org.spacehub.entities.RegistrationRequest;
import org.spacehub.entities.User;
import org.spacehub.entities.UserRole;
import org.spacehub.repository.UserRepository;
import org.spacehub.security.EmailValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegistrationService {

  private final PasswordEncoder passwordEncoder;
  private final EmailValidator emailValidator;
  private final UserRepository userRepository;

  public RegistrationService(PasswordEncoder passwordEncoder, EmailValidator emailValidator,
                             UserRepository userRepository) {
    this.passwordEncoder = passwordEncoder;
    this.emailValidator = emailValidator;
    this.userRepository = userRepository;
  }

  @Transactional
  public User register(RegistrationRequest request) {
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
    newUser.setEnabled(false);
    newUser.setIsVerifiedRegistration(false);
    newUser.setIsVerifiedLogin(false);
    newUser.setIsVerifiedForgot(false);
    userRepository.save(newUser);

    return newUser;
  }
}

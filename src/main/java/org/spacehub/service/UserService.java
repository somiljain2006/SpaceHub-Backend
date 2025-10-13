package org.spacehub.service;

import org.spacehub.entities.User;
import org.spacehub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

  @Autowired
  PasswordEncoder passwordEncoder;

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    return userRepository.findByEmail(email)
      .orElseThrow(() ->
        new UsernameNotFoundException(String.format("User with email %s has not been found", email))
      );
  }

  public boolean checkUser(String email) {
    return userRepository.findByEmail(email).isPresent();
  }

  public void updatePassword(String email, String newPassword) {
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new IllegalStateException("User not found"));
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);
  }

}

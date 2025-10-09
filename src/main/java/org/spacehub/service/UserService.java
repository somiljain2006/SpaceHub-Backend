package org.spacehub.service;


import lombok.AllArgsConstructor;
import org.spacehub.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService implements UserDetailsService {

  private final UserRepository userRepository;
  private static final String Exception_msg = "User with email %s has not been found";

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    return userRepository.findByEmail(email)
      .orElseThrow(() ->
        new UsernameNotFoundException(String.format(Exception_msg, email))
      );
  }
}

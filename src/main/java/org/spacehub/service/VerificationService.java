package org.spacehub.service;

import org.spacehub.entity.User;
import org.spacehub.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;

@Service
public class VerificationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserNameService userNameService;

    @Autowired
    AuthenticationManager authenticationManager;


    public String check(User user) {
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword()));

        if (authentication.isAuthenticated()) {
            return userNameService.generateToken(user);
        }
        else {
            return "null";
        }

    }

}

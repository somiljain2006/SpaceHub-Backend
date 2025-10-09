package org.spacehub.user;

import org.springframework.security.core.userdetails.UserDetails;

public class User{

    private Long id;
    private String name;
    private String username;
    private String email;
    private String password;
    private org.spacehub.authentication.user.UserRole roles;
    private Boolean enabled;
    private Boolean locked;



}

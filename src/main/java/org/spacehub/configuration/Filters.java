package org.spacehub.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.spacehub.service.UserNameService;
import org.spacehub.service.UserService;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class Filters extends OncePerRequestFilter {

  private final UserNameService usernameService;
  private final UserService userService;

  public Filters(UserNameService usernameService, UserService userService) {
    this.usernameService = usernameService;
    this.userService = userService;
  }

  @Override
  protected void doFilterInternal(
    @NonNull HttpServletRequest request,
    @NonNull HttpServletResponse response,
    @NonNull FilterChain filterChain) throws ServletException, IOException {

    final String header = request.getHeader("Authorization");
    String token = null;
    String userEmail = null;

    if (header != null && header.startsWith("Bearer ")) {
      token = header.substring(7);
      userEmail = usernameService.extractUsername(token);
    }

    if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      UserDetails userDetails = userService.loadUserByUsername(userEmail);
      if (usernameService.validToken(token, userDetails)) {
        UsernamePasswordAuthenticationToken authToken =
          new UsernamePasswordAuthenticationToken(userDetails, null,
            userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
      }
    }

    filterChain.doFilter(request, response);
  }
}

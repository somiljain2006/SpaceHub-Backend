package org.spacehub.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.spacehub.service.UserNameService;
import org.spacehub.service.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class Filters extends OncePerRequestFilter {

    UserNameService usernameService;
    private UserService userService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String Header = request.getHeader("Authorization");

        if (Header == null || !Header.startsWith("Bearer ")){
            filterChain.doFilter(request, response);
        }

        final String token = Header.substring(7);

        final String userEmail = usernameService.getUsername(token);

        if (userEmail != null || SecurityContextHolder.getContext().getAuthentication() != null) {
            UserDetails userDetails = this.userService.loadUserByUsername(userEmail);

            if (usernameService.validToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);

            }

        }

        filterChain.doFilter(request, response);

    }
}
package org.spacehub.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.spacehub.service.UserNameService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class Filters extends OncePerRequestFilter {

    UserNameService usernameService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String Header = request.getHeader("Authorization");

        if (Header == null || !Header.startsWith("Bearer ")){
            filterChain.doFilter(request, response);
        }

        final String token = Header.substring(7);

        final String username = usernameService.Username(token);



    }
}
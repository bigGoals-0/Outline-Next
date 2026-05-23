package com.outline.server.security;

import com.outline.server.repository.AuthSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class SessionAuthenticationFilter extends OncePerRequestFilter {
    private final AuthSessionRepository sessions;

    public SessionAuthenticationFilter(AuthSessionRepository sessions) {
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = request.getHeader("X-Session-Token");
        try {
            if (token != null && !token.isBlank()) {
                sessions.findByTokenAndRevokedFalse(token)
                        .filter(session -> session.getExpiresAt().isAfter(Instant.now()))
                        .ifPresent(session -> {
                            CurrentUser.set(session.getUser());
                            SecurityContextHolder.getContext().setAuthentication(
                                    new UsernamePasswordAuthenticationToken(session.getUser().getUsername(), null, java.util.List.of())
                            );
                        });
            }
            chain.doFilter(request, response);
        } finally {
            CurrentUser.clear();
            SecurityContextHolder.clearContext();
        }
    }
}

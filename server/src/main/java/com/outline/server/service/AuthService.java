package com.outline.server.service;

import com.outline.server.auth.AuthSession;
import com.outline.server.dto.AuthDtos;
import com.outline.server.dto.UserResponse;
import com.outline.server.exception.ApiException;
import com.outline.server.repository.AuthSessionRepository;
import com.outline.server.repository.UserRepository;
import com.outline.server.user.User;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.random.RandomGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final AuthSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository users, AuthSessionRepository sessions, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String username = normalize(request.username());
        if (username.length() < 3) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Username must be at least 3 characters");
        }
        if (request.password() == null || request.password().length() < 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
        }
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already exists");
        }
        User user = new User();
        user.setUsername(username);
        user.setDisplayName(blankTo(request.displayName(), username));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setBio("");
        user.setOnline(true);
        users.save(user);
        return createSession(user, false);
    }

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        User user = users.findByUsernameIgnoreCase(normalize(request.username()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        user.setOnline(true);
        users.save(user);
        return createSession(user, request.rememberMe());
    }

    @Transactional
    public Map<String, String> logout(User user, String token) {
        if (token != null) {
            sessions.findByTokenAndRevokedFalse(token).ifPresent(session -> {
                session.setRevoked(true);
                sessions.save(session);
            });
        }
        user.setOnline(false);
        user.setLastSeen(Instant.now());
        users.save(user);
        return Map.of("status", "logged_out");
    }

    private AuthDtos.AuthResponse createSession(User user, boolean rememberMe) {
        byte[] bytes = new byte[32];
        RandomGenerator.getDefault().nextBytes(bytes);
        AuthSession session = new AuthSession();
        session.setToken(HexFormat.of().formatHex(bytes));
        session.setUser(user);
        session.setRememberMe(rememberMe);
        session.setExpiresAt(Instant.now().plus(rememberMe ? Duration.ofDays(30) : Duration.ofHours(12)));
        sessions.save(session);
        return new AuthDtos.AuthResponse(session.getToken(), UserResponse.from(user));
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}

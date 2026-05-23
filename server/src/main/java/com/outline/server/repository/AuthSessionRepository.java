package com.outline.server.repository;

import com.outline.server.auth.AuthSession;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
    Optional<AuthSession> findByTokenAndRevokedFalse(String token);
}

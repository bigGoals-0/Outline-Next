package com.outline.server.repository;

import com.outline.server.user.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    List<User> findTop12ByUsernameContainingIgnoreCaseAndIdNot(String username, Long id);
    List<User> findTop12ByOnlineTrueAndIdNot(Long id);
}

package com.outline.server.service;

import com.outline.server.dto.UserResponse;
import com.outline.server.exception.ApiException;
import com.outline.server.repository.UserRepository;
import com.outline.server.user.User;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    public User byId(Long id) {
        return users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public List<UserResponse> search(User viewer, String query) {
        if (query == null || query.isBlank()) {
            return users.findTop12ByOnlineTrueAndIdNot(viewer.getId()).stream().map(UserResponse::from).toList();
        }
        return users.findTop12ByUsernameContainingIgnoreCaseAndIdNot(query.trim(), viewer.getId())
                .stream().map(UserResponse::from).toList();
    }
}

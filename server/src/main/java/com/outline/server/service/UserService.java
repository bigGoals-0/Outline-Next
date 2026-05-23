package com.outline.server.service;

import com.outline.server.dto.UserResponse;
import com.outline.server.exception.ApiException;
import com.outline.server.repository.UserRepository;
import com.outline.server.user.User;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public UserResponse update(User user, ProfileUpdateRequest request) {
        String displayName = request.displayName() == null ? "" : request.displayName().trim();
        if (displayName.length() < 2 || displayName.length() > 80) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Display name must be between 2 and 80 characters");
        }
        String bio = request.bio() == null ? "" : request.bio().trim();
        if (bio.length() > 280) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bio must be 280 characters or less");
        }
        user.setDisplayName(displayName);
        user.setBio(bio);
        user.setProfilePictureUrl(blankToNull(request.profilePictureUrl()));
        users.save(user);
        return UserResponse.from(user);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record ProfileUpdateRequest(String displayName, String bio, String profilePictureUrl) {}
}

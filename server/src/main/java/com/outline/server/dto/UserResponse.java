package com.outline.server.dto;

import com.outline.server.user.User;
import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String displayName,
        String profilePictureUrl,
        String bio,
        boolean online,
        Instant lastSeen
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getProfilePictureUrl(),
                user.getBio(),
                user.isOnline(),
                user.getLastSeen()
        );
    }
}

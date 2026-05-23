package com.outline.server.dto;

import com.outline.server.friend.Friendship;
import com.outline.server.friend.FriendshipStatus;
import com.outline.server.user.User;
import java.time.Instant;

public final class FriendDtos {
    private FriendDtos() {}

    public record FriendRequestCreate(String username) {}
    public record FriendActionResponse(Long friendshipId, FriendshipStatus status, UserResponse user, Instant createdAt) {
        public static FriendActionResponse from(Friendship friendship, User viewer) {
            User other = friendship.getRequester().getId().equals(viewer.getId())
                    ? friendship.getAddressee()
                    : friendship.getRequester();
            return new FriendActionResponse(
                    friendship.getId(),
                    friendship.getStatus(),
                    UserResponse.from(other),
                    friendship.getCreatedAt()
            );
        }
    }
}

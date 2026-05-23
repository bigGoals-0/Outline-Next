package com.outline.server.dto;

import java.util.List;

public record HomeResponse(
        List<UserResponse> recommendations,
        List<UserResponse> recentlyActive,
        List<FriendDtos.FriendActionResponse> pendingRequests,
        List<UserResponse> onlineFriends
) {}

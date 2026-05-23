package com.outline.client.network.dto;

import java.util.List;

public record HomeResponse(List<UserResponse> recommendations, List<UserResponse> recentlyActive, List<FriendResponse> pendingRequests, List<UserResponse> onlineFriends) {}

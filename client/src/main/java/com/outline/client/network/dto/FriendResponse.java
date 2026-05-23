package com.outline.client.network.dto;

public record FriendResponse(Long friendshipId, String status, UserResponse user, String createdAt) {}

package com.outline.client.network.dto;

public record UserResponse(Long id, String username, String displayName, String profilePictureUrl, String bio, boolean online, String lastSeen) {}

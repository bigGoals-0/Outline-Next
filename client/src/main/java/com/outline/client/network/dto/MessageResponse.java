package com.outline.client.network.dto;

public record MessageResponse(Long id, UserResponse sender, UserResponse recipient, String content, String sentAt, String readAt) {}

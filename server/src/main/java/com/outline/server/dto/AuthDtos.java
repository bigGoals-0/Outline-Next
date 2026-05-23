package com.outline.server.dto;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(String username, String password, String displayName) {}
    public record LoginRequest(String username, String password, boolean rememberMe) {}
    public record AuthResponse(String token, UserResponse user) {}
}

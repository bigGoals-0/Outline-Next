package com.outline.server.controller;

import com.outline.server.dto.AuthDtos;
import com.outline.server.security.CurrentUser;
import com.outline.server.service.AuthService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    AuthDtos.AuthResponse register(@RequestBody AuthDtos.RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    AuthDtos.AuthResponse login(@RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    Map<String, String> logout(@RequestHeader(value = "X-Session-Token", required = false) String token) {
        return authService.logout(CurrentUser.get(), token);
    }

    @GetMapping("/me")
    Object me() {
        return com.outline.server.dto.UserResponse.from(CurrentUser.get());
    }
}

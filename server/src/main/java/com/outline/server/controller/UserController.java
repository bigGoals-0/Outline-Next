package com.outline.server.controller;

import com.outline.server.dto.UserResponse;
import com.outline.server.security.CurrentUser;
import com.outline.server.service.UserService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/search")
    List<UserResponse> search(@RequestParam(defaultValue = "") String q) {
        return userService.search(CurrentUser.get(), q);
    }

    @GetMapping("/me")
    UserResponse me() {
        return UserResponse.from(CurrentUser.get());
    }

    @PutMapping("/me")
    UserResponse update(@RequestBody UserService.ProfileUpdateRequest request) {
        return userService.update(CurrentUser.get(), request);
    }
}

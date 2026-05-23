package com.outline.server.controller;

import com.outline.server.dto.FriendDtos;
import com.outline.server.security.CurrentUser;
import com.outline.server.service.FriendService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/friends")
public class FriendController {
    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @GetMapping
    List<FriendDtos.FriendActionResponse> friends() {
        return friendService.friends(CurrentUser.get());
    }

    @GetMapping("/pending")
    List<FriendDtos.FriendActionResponse> pending() {
        return friendService.pending(CurrentUser.get());
    }

    @PostMapping("/requests")
    FriendDtos.FriendActionResponse request(@RequestBody FriendDtos.FriendRequestCreate request) {
        return friendService.request(CurrentUser.get(), request.username());
    }

    @PostMapping("/{id}/accept")
    FriendDtos.FriendActionResponse accept(@PathVariable Long id) {
        return friendService.respond(CurrentUser.get(), id, true);
    }

    @PostMapping("/{id}/decline")
    FriendDtos.FriendActionResponse decline(@PathVariable Long id) {
        return friendService.respond(CurrentUser.get(), id, false);
    }

    @DeleteMapping("/{id}")
    Map<String, String> remove(@PathVariable Long id) {
        friendService.remove(CurrentUser.get(), id);
        return Map.of("status", "removed");
    }
}

package com.outline.server.controller;

import com.outline.server.dto.HomeResponse;
import com.outline.server.dto.UserResponse;
import com.outline.server.security.CurrentUser;
import com.outline.server.service.FriendService;
import java.util.Comparator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
public class HomeController {
    private final FriendService friendService;

    public HomeController(FriendService friendService) {
        this.friendService = friendService;
    }

    @GetMapping
    HomeResponse home() {
        var user = CurrentUser.get();
        var online = friendService.onlineFriends(user);
        var recent = friendService.friends(user).stream()
                .map(friend -> friend.user())
                .sorted(Comparator.comparing(UserResponse::lastSeen, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .toList();
        return new HomeResponse(friendService.recommendations(user), recent, friendService.pending(user), online);
    }
}

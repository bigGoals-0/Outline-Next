package com.outline.server.service;

import com.outline.server.dto.FriendDtos;
import com.outline.server.dto.UserResponse;
import com.outline.server.exception.ApiException;
import com.outline.server.friend.Friendship;
import com.outline.server.friend.FriendshipStatus;
import com.outline.server.repository.FriendshipRepository;
import com.outline.server.repository.UserRepository;
import com.outline.server.user.User;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendService {
    private final FriendshipRepository friendships;
    private final UserRepository users;

    public FriendService(FriendshipRepository friendships, UserRepository users) {
        this.friendships = friendships;
        this.users = users;
    }

    @Transactional
    public FriendDtos.FriendActionResponse request(User requester, String username) {
        User addressee = users.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (requester.getId().equals(addressee.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot add yourself");
        }
        friendships.findActiveBetween(requester, addressee).ifPresent(existing -> {
            throw new ApiException(HttpStatus.CONFLICT, "Friendship already exists");
        });
        Friendship friendship = new Friendship();
        friendship.setRequester(requester);
        friendship.setAddressee(addressee);
        friendship.setStatus(FriendshipStatus.PENDING);
        friendships.save(friendship);
        return FriendDtos.FriendActionResponse.from(friendship, requester);
    }

    @Transactional
    public FriendDtos.FriendActionResponse respond(User user, Long friendshipId, boolean accept) {
        Friendship friendship = friendships.findById(friendshipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found"));
        if (!friendship.getAddressee().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the addressee can respond");
        }
        friendship.setStatus(accept ? FriendshipStatus.ACCEPTED : FriendshipStatus.DECLINED);
        friendship.setRespondedAt(Instant.now());
        friendships.save(friendship);
        return FriendDtos.FriendActionResponse.from(friendship, user);
    }

    @Transactional
    public void remove(User user, Long friendshipId) {
        Friendship friendship = friendships.findById(friendshipId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Friendship not found"));
        if (!friendship.getRequester().getId().equals(user.getId()) && !friendship.getAddressee().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your friendship");
        }
        friendships.delete(friendship);
    }

    public List<FriendDtos.FriendActionResponse> friends(User user) {
        return friendships.findConnections(user, FriendshipStatus.ACCEPTED).stream()
                .map(friendship -> FriendDtos.FriendActionResponse.from(friendship, user))
                .toList();
    }

    public List<FriendDtos.FriendActionResponse> pending(User user) {
        return friendships.findByAddresseeAndStatus(user, FriendshipStatus.PENDING).stream()
                .map(friendship -> FriendDtos.FriendActionResponse.from(friendship, user))
                .toList();
    }

    public List<UserResponse> onlineFriends(User user) {
        return friendships.findConnections(user, FriendshipStatus.ACCEPTED).stream()
                .map(friendship -> friendship.getRequester().getId().equals(user.getId()) ? friendship.getAddressee() : friendship.getRequester())
                .filter(User::isOnline)
                .map(UserResponse::from)
                .toList();
    }

    public List<UserResponse> recommendations(User user) {
        LinkedHashSet<User> recommended = new LinkedHashSet<>();
        users.findTop12ByUsernameContainingIgnoreCaseAndIdNot(user.getUsername().substring(0, Math.min(2, user.getUsername().length())), user.getId())
                .forEach(recommended::add);
        users.findTop12ByOnlineTrueAndIdNot(user.getId()).forEach(recommended::add);
        return recommended.stream()
                .filter(candidate -> friendships.findActiveBetween(user, candidate).isEmpty())
                .limit(8)
                .map(UserResponse::from)
                .toList();
    }
}

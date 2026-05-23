package com.outline.server.repository;

import com.outline.server.friend.Friendship;
import com.outline.server.friend.FriendshipStatus;
import com.outline.server.user.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    @Query("""
        select f from Friendship f
        where ((f.requester = :a and f.addressee = :b) or (f.requester = :b and f.addressee = :a))
        and f.status <> com.outline.server.friend.FriendshipStatus.DECLINED
        """)
    Optional<Friendship> findActiveBetween(@Param("a") User a, @Param("b") User b);

    List<Friendship> findByAddresseeAndStatus(User addressee, FriendshipStatus status);
    List<Friendship> findByRequesterAndStatus(User requester, FriendshipStatus status);

    @Query("""
        select f from Friendship f
        where (f.requester = :user or f.addressee = :user) and f.status = :status
        order by f.respondedAt desc nulls last, f.createdAt desc
        """)
    List<Friendship> findConnections(@Param("user") User user, @Param("status") FriendshipStatus status);
}

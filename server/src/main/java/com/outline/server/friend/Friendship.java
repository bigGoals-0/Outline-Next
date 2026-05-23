package com.outline.server.friend;

import com.outline.server.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "friendships")
public class Friendship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User requester;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User addressee;

    @Enumerated(EnumType.STRING)
    private FriendshipStatus status;

    private Instant createdAt;
    private Instant respondedAt;

    @PrePersist
    void timestamp() {
        createdAt = Instant.now();
    }
}

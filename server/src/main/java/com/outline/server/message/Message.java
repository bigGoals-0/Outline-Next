package com.outline.server.message;

import com.outline.server.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User sender;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User recipient;

    @Column(nullable = false, length = 4000)
    private String content;

    private Instant sentAt;
    private Instant readAt;

    @PrePersist
    void timestamp() {
        sentAt = Instant.now();
    }
}

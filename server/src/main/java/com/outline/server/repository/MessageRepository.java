package com.outline.server.repository;

import com.outline.server.message.Message;
import com.outline.server.user.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("""
        select m from Message m
        where (m.sender = :a and m.recipient = :b) or (m.sender = :b and m.recipient = :a)
        order by m.sentAt asc
        """)
    List<Message> conversation(@Param("a") User a, @Param("b") User b);
}

package com.outline.server.dto;

import com.outline.server.message.Message;
import java.time.Instant;

public final class MessageDtos {
    private MessageDtos() {}

    public record SendMessageRequest(Long recipientId, String content) {}
    public record TypingEvent(Long recipientId, boolean typing) {}
    public record MessageResponse(Long id, UserResponse sender, UserResponse recipient, String content, Instant sentAt, Instant readAt) {
        public static MessageResponse from(Message message) {
            return new MessageResponse(
                    message.getId(),
                    UserResponse.from(message.getSender()),
                    UserResponse.from(message.getRecipient()),
                    message.getContent(),
                    message.getSentAt(),
                    message.getReadAt()
            );
        }
    }
}

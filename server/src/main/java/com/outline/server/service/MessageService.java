package com.outline.server.service;

import com.outline.server.dto.MessageDtos;
import com.outline.server.message.Message;
import com.outline.server.repository.MessageRepository;
import com.outline.server.user.User;
import java.time.Instant;
import java.util.List;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageService {
    private final MessageRepository messages;
    private final UserService users;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageService(MessageRepository messages, UserService users, SimpMessagingTemplate messagingTemplate) {
        this.messages = messages;
        this.users = users;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public MessageDtos.MessageResponse send(User sender, MessageDtos.SendMessageRequest request) {
        User recipient = users.byId(request.recipientId());
        Message message = new Message();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(request.content() == null ? "" : request.content().trim());
        messages.save(message);
        MessageDtos.MessageResponse response = MessageDtos.MessageResponse.from(message);
        messagingTemplate.convertAndSend("/topic/user." + sender.getId(), response);
        messagingTemplate.convertAndSend("/topic/user." + recipient.getId(), response);
        return response;
    }

    @Transactional
    public List<MessageDtos.MessageResponse> conversation(User viewer, Long otherId) {
        User other = users.byId(otherId);
        List<Message> conversation = messages.conversation(viewer, other);
        conversation.stream()
                .filter(message -> message.getRecipient().getId().equals(viewer.getId()) && message.getReadAt() == null)
                .forEach(message -> message.setReadAt(Instant.now()));
        return conversation.stream().map(MessageDtos.MessageResponse::from).toList();
    }

    public void typing(User sender, MessageDtos.TypingEvent event) {
        messagingTemplate.convertAndSend("/topic/user." + event.recipientId(), event);
    }
}

package com.outline.server.controller;

import com.outline.server.dto.MessageDtos;
import com.outline.server.security.CurrentUser;
import com.outline.server.service.MessageService;
import java.util.List;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    MessageDtos.MessageResponse send(@RequestBody MessageDtos.SendMessageRequest request) {
        return messageService.send(CurrentUser.get(), request);
    }

    @GetMapping("/conversation/{userId}")
    List<MessageDtos.MessageResponse> conversation(@PathVariable Long userId) {
        return messageService.conversation(CurrentUser.get(), userId);
    }

    @MessageMapping("/typing")
    void typing(MessageDtos.TypingEvent event) {
        var sender = CurrentUser.get();
        if (sender != null) {
            messageService.typing(sender, event);
        }
    }
}

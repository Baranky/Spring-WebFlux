package com.reactivechat.chat.controller;

import com.reactivechat.chat.dto.MessageDto;
import com.reactivechat.chat.service.MessageService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/{roomId}/messages")
    public Flux<MessageDto> getHistory(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "50") int limit,
            Authentication auth) {
        String userId = (String) auth.getPrincipal();
        return messageService.getHistory(roomId, userId, limit);
    }
}

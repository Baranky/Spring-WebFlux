package com.reactivechat.chat.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reactivechat.chat.dto.MessageDto;
import com.reactivechat.chat.service.MessageService;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.Map;


@Component
public class ChatWebSocketHandler implements WebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    private final JwtValidationService jwtValidationService;

    public ChatWebSocketHandler(MessageService messageService,
                                ObjectMapper objectMapper,
                                JwtValidationService jwtValidationService) {
        this.messageService = messageService;
        this.objectMapper = objectMapper;
        this.jwtValidationService = jwtValidationService;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String token = tokenFromHandshake(session);
        String roomId = roomIdFromHandshake(session);
        if (token == null || roomId == null) {
            return session.close();
        }
                return jwtValidationService.validateAndGetUserId(token)
                .flatMap(userId -> {
                    var outbound = messageService.subscribeToRoom(roomId)
                            .map(dto -> toJson(dto))
                            .map(session::textMessage)
                            .onErrorResume(e -> reactor.core.publisher.Flux.never());
                    var inbound = session.receive()
                            .map(msg -> msg.getPayloadAsText())
                            .flatMap(text -> parseAndProcess(text, userId, roomId).then())
                            .onErrorContinue((e, o) -> { });
                    return session.send(outbound).and(inbound);
                })
                .onErrorResume(e -> session.close());
    }

    private String tokenFromHandshake(WebSocketSession session) {
        String auth = session.getHandshakeInfo().getHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth;
        }
        String query = session.getHandshakeInfo().getUri().getQuery();
        if (query != null && query.contains("token=")) {
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    return "Bearer " + param.substring(6);
                }
            }
        }
        return null;
    }

    private String roomIdFromHandshake(WebSocketSession session) {
        String query = session.getHandshakeInfo().getUri().getQuery();
        if (query == null) return null;
        for (String param : query.split("&")) {
            if (param.startsWith("roomId=")) {
                return param.substring(7);
            }
        }
        return null;
    }

    private String toJson(MessageDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Mono<MessageDto> parseAndProcess(String text, String userId, String handshakeRoomId) {
        log.debug("WebSocket gelen ham mesaj: {}", text);
        if (text == null || text.isBlank()) {
            return Mono.empty();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(text, Map.class);
            String roomId = handshakeRoomId;
            Object roomObj = map.get("roomId");
            if (roomObj != null && !roomObj.toString().isBlank()) {
                roomId = roomObj.toString();
            }
            Object contentObj = map.get("content");
            String content = contentObj != null ? contentObj.toString() : null;
            if (roomId == null || content == null || content.isBlank()) {
                log.debug("roomId veya content bos, atlanıyor. roomId={}, content={}", roomId, content);
                return Mono.empty();
            }
            return messageService.processMessage(userId, roomId, content);
        } catch (Exception e) {
            log.warn("WebSocket mesaj parse/process hatası: {} - {}", e.getMessage(), text);
            return Mono.error(e);
        }
    }
}

package com.reactivechat.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reactivechat.chat.dto.MessageDto;
import com.reactivechat.chat.entity.Message;
import com.reactivechat.chat.entity.Room;
import com.reactivechat.chat.config.RedisPubSubConfig;
import com.reactivechat.chat.repository.MessageRepository;
import com.reactivechat.chat.repository.RoomRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.connection.ReactiveSubscription;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public MessageService(MessageRepository messageRepository,
                          RoomRepository roomRepository,
                          @Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate,
                          ObjectMapper objectMapper) {
        this.messageRepository = messageRepository;
        this.roomRepository = roomRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }


    public Mono<MessageDto> processMessage(String senderId, String roomId, String content) {
        return roomRepository.existsByIdAndMemberIdsContaining(roomId, senderId)
                .flatMap(isMember -> Boolean.TRUE.equals(isMember)
                        ? Mono.just(true)
                        : ensureUserInRoom(roomId, senderId))
                .then(Mono.defer(() -> {
                    Message msg = new Message();
                    msg.setRoomId(roomId);
                    msg.setSenderId(senderId);
                    msg.setContent(content);
                    return messageRepository.save(msg);
                }))
                .flatMap(saved -> broadcastToRedis(saved).thenReturn(MessageDto.from(saved)));
    }

    private Mono<Long> broadcastToRedis(Message message) {
        String channel = RedisPubSubConfig.roomChannel(message.getRoomId());
        try {
            String payload = objectMapper.writeValueAsString(MessageDto.from(message));
            return redisTemplate.convertAndSend(channel, payload).defaultIfEmpty(0L);
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }


    public Flux<MessageDto> getHistory(String roomId, String userId, int limit) {
        return roomRepository.existsByIdAndMemberIdsContaining(roomId, userId)
                .flatMap(isMember -> Boolean.TRUE.equals(isMember)
                        ? Mono.just(true)
                        : ensureUserInRoom(roomId, userId))
                .thenMany(messageRepository.findByRoomIdOrderByTimestampDesc(roomId, PageRequest.of(0, limit)))
                .map(m -> MessageDto.forViewer(m, userId));
    }

    private Mono<Boolean> ensureUserInRoom(String roomId, String userId) {
        return roomRepository.findById(roomId)
                .flatMap(room -> {
                    Set<String> members = room.getMemberIds();
                    if (members == null) {
                        room.setMemberIds(new java.util.HashSet<>(Set.of(userId)));
                        return roomRepository.save(room).thenReturn(true);
                    }
                    if (!members.contains(userId)) {
                        Set<String> newSet = new java.util.HashSet<>(members);
                        newSet.add(userId);
                        room.setMemberIds(newSet);
                        return roomRepository.save(room).thenReturn(true);
                    }
                    return Mono.just(true);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    Room newRoom = new Room();
                    newRoom.setId(roomId);
                    newRoom.setName(roomId);
                    newRoom.setMemberIds(new java.util.HashSet<>(Set.of(userId)));
                    return roomRepository.save(newRoom).thenReturn(true);
                }));
    }


    public Flux<MessageDto> subscribeToRoom(String roomId) {
        ChannelTopic topic = ChannelTopic.of(RedisPubSubConfig.roomChannel(roomId));
        return redisTemplate.listenTo(topic)
                .map(ReactiveSubscription.Message::getMessage)
                .map(s -> {
                    try {
                        return objectMapper.readValue(s, MessageDto.class);
                    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}

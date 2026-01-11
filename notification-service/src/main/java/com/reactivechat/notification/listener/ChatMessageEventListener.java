package com.reactivechat.notification.listener;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.reactivechat.notification.config.NotificationChannels;
import com.reactivechat.notification.service.NotificationDeliveryService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import reactor.core.publisher.Flux;

import java.util.Map;


@Configuration
public class ChatMessageEventListener {

    @Bean
    public ApplicationRunner startNotificationListener(
            @Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate,
            NotificationDeliveryService deliveryService,
            ObjectMapper objectMapper) {
        return args -> {
            Flux<String> messages = redisTemplate
                    .listenTo(ChannelTopic.of(NotificationChannels.NOTIFICATION_OFFLINE))
                    .map(msg -> msg.getMessage());
            messages
                    .flatMap(json -> processOfflineNotification(json, deliveryService, objectMapper))
                    .subscribe();
        };
    }

    private static reactor.core.publisher.Mono<Void> processOfflineNotification(
            String json, NotificationDeliveryService deliveryService, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(json, Map.class);
            String userId = (String) payload.get("userId");
            String messagePreview = (String) payload.get("messagePreview");
            String roomId = (String) payload.get("roomId");
            if (userId != null && messagePreview != null) {
                return deliveryService.sendPushToUser(userId, messagePreview, roomId);
            }
        } catch (Exception e) {

        }
        return reactor.core.publisher.Mono.empty();
    }

}

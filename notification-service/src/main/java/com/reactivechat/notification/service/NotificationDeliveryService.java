package com.reactivechat.notification.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Service
public class NotificationDeliveryService {

    public Mono<Void> sendPushToUser(String userId, String messagePreview, String roomId) {
        return Mono.fromRunnable(() -> {
        }).then();
    }
}

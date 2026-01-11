package com.reactivechat.chat.repository;

import com.reactivechat.chat.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface MessageRepository extends ReactiveMongoRepository<Message, String> {

    Flux<Message> findByRoomIdOrderByTimestampDesc(String roomId, Pageable pageable);
}

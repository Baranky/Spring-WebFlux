package com.reactivechat.chat.repository;

import com.reactivechat.chat.entity.Room;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RoomRepository extends ReactiveMongoRepository<Room, String> {

    Flux<Room> findByMemberIdsContaining(String userId);
    Mono<Boolean> existsByIdAndMemberIdsContaining(String roomId, String userId);
}

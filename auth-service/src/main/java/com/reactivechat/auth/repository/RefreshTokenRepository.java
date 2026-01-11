package com.reactivechat.auth.repository;

import com.reactivechat.auth.entity.RefreshToken;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface RefreshTokenRepository extends ReactiveMongoRepository<RefreshToken, String> {

    Mono<RefreshToken> findByToken(String token);
    Mono<Void> deleteByUserId(String userId);
}

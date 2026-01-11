package com.reactivechat.presence.service;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;


@Service
public class PresenceService {

    private static final String ONLINE_SET_KEY = "presence:online";
    private static final String LAST_SEEN_PREFIX = "presence:lastseen:";
    private static final Duration HEARTBEAT_TTL = Duration.ofMinutes(5);

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public PresenceService(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Void> setOnline(String userId) {
        String now = Instant.now().toString();
        return redisTemplate.opsForSet().add(ONLINE_SET_KEY, userId)
                .then(redisTemplate.opsForValue().set(LAST_SEEN_PREFIX + userId, now, HEARTBEAT_TTL))
                .then();
    }

    public Mono<Void> heartbeat(String userId) {
        return redisTemplate.opsForValue().set(LAST_SEEN_PREFIX + userId, Instant.now().toString(), HEARTBEAT_TTL)
                .then();
    }

    public Mono<Void> setOffline(String userId) {
        return redisTemplate.opsForSet().remove(ONLINE_SET_KEY, userId)
                .then(redisTemplate.opsForValue().getAndDelete(LAST_SEEN_PREFIX + userId))
                .then();
    }

    public Mono<Boolean> isOnline(String userId) {
        return redisTemplate.opsForSet().isMember(ONLINE_SET_KEY, userId);
    }

    public Mono<Instant> getLastSeen(String userId) {
        return redisTemplate.opsForValue().get(LAST_SEEN_PREFIX + userId)
                .map(Instant::parse);
    }

    public Mono<Set<String>> getOnlineUserIds() {
        return redisTemplate.opsForSet().members(ONLINE_SET_KEY)
                .collect(Collectors.toSet());
    }
}

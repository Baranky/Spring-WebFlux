package com.reactivechat.presence.controller;

import com.reactivechat.presence.service.PresenceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    private static final String X_USER_ID = "X-User-Id";

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @PostMapping("/online")
    public Mono<Void> setOnline(@RequestHeader(value = X_USER_ID, required = false) String userIdFromHeader,
                                @RequestParam(required = false) String userId) {
        String uid = userIdFromHeader != null ? userIdFromHeader : userId;
        if (uid == null || uid.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing userId (X-User-Id header or userId param)"));
        }
        return presenceService.setOnline(uid);
    }

    @PostMapping("/heartbeat")
    public Mono<Void> heartbeat(@RequestHeader(value = X_USER_ID, required = false) String userIdFromHeader,
                                @RequestParam(required = false) String userId) {
        String uid = userIdFromHeader != null ? userIdFromHeader : userId;
        if (uid == null || uid.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing userId (X-User-Id header or userId param)"));
        }
        return presenceService.heartbeat(uid);
    }

    @PostMapping("/offline")
    public Mono<Void> setOffline(@RequestHeader(value = X_USER_ID, required = false) String userIdFromHeader,
                                 @RequestParam(required = false) String userId) {
        String uid = userIdFromHeader != null ? userIdFromHeader : userId;
        if (uid == null || uid.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing userId (X-User-Id header or userId param)"));
        }
        return presenceService.setOffline(uid);
    }

    @GetMapping("/{userId}/status")
    public Mono<Map<String, Object>> status(@PathVariable String userId) {
        return presenceService.isOnline(userId)
                .zipWith(presenceService.getLastSeen(userId).defaultIfEmpty(Instant.EPOCH))
                .map(tuple -> Map.<String, Object>of(
                        "userId", userId,
                        "online", tuple.getT1(),
                        "lastSeen", tuple.getT2().toString()
                ));
    }

    @GetMapping("/online")
    public Mono<Set<String>> onlineUsers() {
        return presenceService.getOnlineUserIds();
    }
}

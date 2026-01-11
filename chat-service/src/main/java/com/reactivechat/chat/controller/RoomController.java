package com.reactivechat.chat.controller;

import com.reactivechat.chat.entity.Room;
import com.reactivechat.chat.repository.RoomRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Room> createRoom(@RequestBody CreateRoomRequest request, Authentication auth) {
        String userId = (String) auth.getPrincipal();
        Room room = new Room();
        room.setName(request.name());
        room.setMemberIds(Set.of(userId));
        return roomRepository.save(room);
    }

    @GetMapping
    public Flux<Room> myRooms(Authentication auth) {
        String userId = (String) auth.getPrincipal();
        return roomRepository.findByMemberIdsContaining(userId);
    }

    public record CreateRoomRequest(String name) {}
}

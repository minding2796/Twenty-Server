package com.thinkinggms.twenty_backend.component;

import com.thinkinggms.twenty_backend.statics.MatchRoom;
import com.thinkinggms.twenty_backend.statics.MatchRoom.RoomStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class MatchRoomRepository {
    public final List<MatchRoom> rooms = new ArrayList<>();

    public Optional<MatchRoom> findByRoomCode(String roomCode) {
        return rooms.stream().filter(r -> r.getRoomCode().equals(roomCode)).findFirst();
    }

    public List<MatchRoom> findByStatus(RoomStatus status) {
        return rooms.stream().filter(r -> r.getStatus() == status).toList();
    }

    public boolean existsByRoomCode(String roomCode) {
        return rooms.stream().anyMatch(r -> r.getRoomCode().equals(roomCode));
    }

    public List<MatchRoom> findAll() {
        return rooms;
    }

    public void delete(MatchRoom room) {
        rooms.remove(room);
    }

    public MatchRoom save(MatchRoom room) {
        rooms.remove(room);
        rooms.add(room);
        return room;
    }
}
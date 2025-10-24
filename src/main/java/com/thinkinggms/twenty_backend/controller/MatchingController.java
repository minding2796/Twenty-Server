package com.thinkinggms.twenty_backend.controller;

import com.thinkinggms.twenty_backend.dto.RoomResponse;
import com.thinkinggms.twenty_backend.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matching")
@RequiredArgsConstructor
public class MatchingController {
    private final MatchingService matchingService;

    /**
     * 대기 중인 방 목록 조회
     */
    @GetMapping("/waiting")
    public ResponseEntity<List<RoomResponse>> getWaitingRooms() {
        List<RoomResponse> rooms = matchingService.getWaitingRooms();
        return ResponseEntity.ok(rooms);
    }

    /**
     * 방 정보 조회
     */
    @GetMapping("/{roomCode}")
    public ResponseEntity<RoomResponse> getRoomByCode(@PathVariable String roomCode) {
        RoomResponse response = matchingService.getRoomByCode(roomCode);
        return ResponseEntity.ok(response);
    }
}
package com.thinkinggms.twenty_backend.service;

import com.thinkinggms.twenty_backend.statics.MatchRoom;
import com.thinkinggms.twenty_backend.domain.User;
import com.thinkinggms.twenty_backend.dto.RoomResponse;
import com.thinkinggms.twenty_backend.component.MatchRoomRepository;
import com.thinkinggms.twenty_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MatchingService {

    private final MatchRoomRepository roomRepository;
    private final Random random = ThreadLocalRandom.current();

    /**
     * 새로운 방 생성
     */
    public MatchRoom match(User user) {
        List<MatchRoom> rooms = roomRepository.findByStatus(MatchRoom.RoomStatus.WAITING);
        if (!rooms.isEmpty()) {
            MatchRoom room = rooms.get(0);
            room.getPlayerEmails().add(user.getEmail());
            room.setCurrentPlayers(room.getCurrentPlayers() + 1);
            if (room.getCurrentPlayers().equals(room.getMaxPlayers())) room.setStatus(MatchRoom.RoomStatus.FULL);
            return roomRepository.save(room);
        }
        // 방 코드 생성 (6자리 랜덤)
        String roomCode = generateRoomCode();

        // 방 생성
        MatchRoom room = MatchRoom.builder()
                .roomCode(roomCode)
                .maxPlayers(2)
                .currentPlayers(1)
                .status(MatchRoom.RoomStatus.WAITING)
                .stockPrice(20)
                .priceHistory(new LinkedList<>())
                .costOfLiving(2)
                .maxColCooldown(5)
                .colCooldown(5)
                .maxCooldown(3)
                .playerEmails(new ArrayList<>())
                .sessions(new ArrayList<>())
                .playerStatus(new ArrayList<>())
                .onNextTurnActions(new ArrayList<>())
                .build();
        room.getPlayerEmails().add(user.getEmail());

        return roomRepository.save(room);
    }

    /**
     * 방 나가기
     */
    public void leaveRoom(User user) {
        MatchRoom room = getRoomContainsUser(user);

        // 방에서 플레이어 제거
        room.getPlayerEmails().remove(user.getEmail());
        room.setCurrentPlayers(room.getCurrentPlayers() - 1);

        // 방이 비었으면 삭제
        if (room.getCurrentPlayers() == 0) {
            roomRepository.delete(room);
        } else {
            // 방 상태 업데이트
            if (room.getStatus() == MatchRoom.RoomStatus.FULL) {
                room.setStatus(MatchRoom.RoomStatus.WAITING);
            }
            roomRepository.save(room);
        }
    }

    /**
     * 대기 중인 방 목록 조회
     */
    @Transactional(readOnly = true)
    public List<RoomResponse> getWaitingRooms() {
        return roomRepository.findByStatus(MatchRoom.RoomStatus.WAITING)
                .stream()
                .map(RoomResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 방 정보 조회
     */
    @Transactional(readOnly = true)
    public RoomResponse getRoomByCode(String roomCode) {
        MatchRoom room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));
        return RoomResponse.from(room);
    }

    // Helper methods

    public MatchRoom getRoomContainsUser(User user) {
        List<MatchRoom> rooms = roomRepository.findAll().stream().filter(r -> r.getPlayerEmails().contains(user.getEmail())).toList();
        if (rooms.isEmpty()) throw new IllegalArgumentException("플레이어가 존재하는 방이 없습니다.");
        return rooms.get(0);
    }

    public String generateRoomCode() {
        String code;
        do code = String.format("%06d", random.nextInt(1000000)); while (roomRepository.existsByRoomCode(code));
        return code;
    }
}
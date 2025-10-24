package com.thinkinggms.twenty_backend.statics;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchRoom {
    private String roomCode;
    private Integer maxPlayers;
    private Integer currentPlayers;
    private RoomStatus status;

    private Integer stockPrice;
    private LinkedList<Integer> priceHistory;
    private Integer costOfLiving;
    private Integer maxColCooldown;
    private Integer colCooldown;
    private Integer maxCooldown;

    private List<String> playerEmails = new ArrayList<>();
    private List<WebSocketSession> sessions = new ArrayList<>();
    private List<PlayerStatus> playerStatus = new ArrayList<>();
    private List<Runnable> onNextTurnActions = new ArrayList<>();

    public enum RoomStatus {
        WAITING,    // 대기 중
        FULL,       // 인원 충족
        IN_PROGRESS // 진행 중
    }

    @Getter
    @Setter
    @Builder
    public static class PlayerStatus {
        private String userEmail;
        private Integer currentFunds;
        private Boolean isReady;
        private Integer goStack;
        private Integer cooldown;
    }

    @PrePersist
    protected void onCreate() {
        if (currentPlayers == null) currentPlayers = 0;
        if (status == null) status = RoomStatus.WAITING;
    }
}
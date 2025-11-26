package com.thinkinggms.twenty_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkinggms.twenty_backend.domain.User;
import com.thinkinggms.twenty_backend.dto.GameData;
import com.thinkinggms.twenty_backend.dto.StockResponse;
import com.thinkinggms.twenty_backend.repository.UserRepository;
import com.thinkinggms.twenty_backend.statics.MatchRoom;
import com.thinkinggms.twenty_backend.component.WebSocketHandler;
import com.thinkinggms.twenty_backend.utils.WebSocketUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class GameService {
    // 주식 가격에 관여하는 변수
    private final int basePrice = 20;
    private final UserRepository userRepository;
    private final MatchingService matchingService;
    private final ObjectMapper objectMapper;

    public List<MatchRoom.PlayerStatus> getPlayerStatus(User user) {
        System.out.println("getPlayerStatus: " + user);
        MatchRoom room = matchingService.getRoomContainsUser(user);
        return room.getPlayerStatus().stream().sorted(Comparator.comparingLong(ps -> Math.abs(ps.getUserEmail().compareTo(user.getEmail())))).toList();
    }

    public StockResponse getStockStatus(User user) {
        System.out.println("getStockStatus: " + user);
        MatchRoom room = matchingService.getRoomContainsUser(user);
        return StockResponse.from(room);
    }

    public void initializeRoom(MatchRoom room) {
        room.setStatus(MatchRoom.RoomStatus.IN_PROGRESS);
        room.setStockPrice(basePrice);
        for (int i = 0; i < 49; i++) {
            nextPrice(room);
        }
        room.setStockPrice(basePrice);
        room.getPriceHistory().addFirst(room.getStockPrice());
    }

    public void buyStock(MatchRoom room, User user, int amount) {
        MatchRoom.PlayerStatus ps = getPlayerStatus(user).get(0);
        ps.setCurrentFunds(ps.getCurrentFunds() - amount * room.getStockPrice());
        room.getOnNextTurnActions().add(() -> {
            ps.setCurrentFunds(ps.getCurrentFunds() + amount * room.getStockPrice());
            ps.setCooldown(room.getMaxCooldown());
        });
    }

    public void sellStock(MatchRoom room, User user, int amount) {
        MatchRoom.PlayerStatus ps = getPlayerStatus(user).get(0);
        ps.setCurrentFunds(ps.getCurrentFunds() + amount * room.getStockPrice());
        room.getOnNextTurnActions().add(() -> {
            ps.setCurrentFunds(ps.getCurrentFunds() - amount * room.getStockPrice());
            ps.setCooldown(room.getMaxCooldown());
        });
    }

    public void onNextTurn(MatchRoom room) {
        nextPrice(room);

        room.setColCooldown(room.getColCooldown() - 1);
        if (room.getColCooldown() <= 0)
        {
            room.getPlayerStatus().forEach(ps -> ps.setCurrentFunds(ps.getCurrentFunds() - room.getCostOfLiving()));
            room.setColCooldown(room.getMaxColCooldown());
        }

        room.getPlayerStatus().forEach(ps -> {
            if (ps.getCooldown() > 0) ps.setCooldown(ps.getCooldown() - 1);
        });

        room.getPlayerStatus().forEach(ps -> ps.setIsReady(false));
        room.getOnNextTurnActions().forEach(Runnable::run);
        room.getOnNextTurnActions().clear();

        room.getPlayerStatus().forEach(ps -> {
            if (ps.getCurrentFunds() < 0) ps.setGoStack(ps.getGoStack() - 1);
            else ps.setGoStack(3);
        });

        if (
                room.getPlayerStatus().stream().allMatch(ps -> ps.getGoStack() <= 0) ||
                room.getPlayerStatus().stream().allMatch(ps -> ps.getCurrentFunds() >= 200)
        ) {
            WebSocketUtils.sendCommand(room.getSessions(),
                    WebSocketHandler.CommandMessage.builder()
                            .command("game/game_set")
                            .data("DRAW")
                            .build()
            );
            room.getPlayerStatus().forEach(ps -> matchingService.leaveRoom(userRepository.findByEmail(ps.getUserEmail()).orElseThrow()));
        }

        List<MatchRoom.PlayerStatus> winner = room.getPlayerStatus().stream().filter(ps -> ps.getCurrentFunds() >= 200).toList();
        if (!winner.isEmpty()) {
            User winnerUser = userRepository.findByEmail(winner.get(0).getUserEmail()).orElseThrow();
            WebSocketUtils.sendCommand(room.getSessions(),
                    WebSocketHandler.CommandMessage.builder()
                            .command("game/game_set")
                            .data("WIN-" + winnerUser.getEmail())
                            .build()
            );
            room.getPlayerStatus().forEach(ps -> matchingService.leaveRoom(userRepository.findByEmail(ps.getUserEmail()).orElseThrow()));
        }

        List<MatchRoom.PlayerStatus> loser = room.getPlayerStatus().stream().filter(ps -> ps.getGoStack() <= 0).toList();
        if (!loser.isEmpty()) {
            User loserUser = userRepository.findByEmail(loser.get(0).getUserEmail()).orElseThrow();
            WebSocketUtils.sendCommand(room.getSessions(),
                    WebSocketHandler.CommandMessage.builder()
                            .command("game/game_set")
                            .data("LOSE-" + loserUser.getEmail())
                            .build()
            );
            room.getPlayerStatus().forEach(ps -> matchingService.leaveRoom(userRepository.findByEmail(ps.getUserEmail()).orElseThrow()));
        }
    }

    /**
     * 세션 기준으로 해당 유저를 패배 처리한다.
     * - 세션에서 User를 찾고
     * - 속한 방을 조회한 뒤
     * - 방 전체에 패배 메시지 브로드캐스트
     * - 방에 있는 모든 플레이어를 매칭에서 제거
     */
    public void lose(WebSocketSession session) {
        // 로그인 정보가 없으면 아무 것도 하지 않음
        if (!(session.getAttributes().get("userPrincipal") instanceof User user)) return;

        try {
            MatchRoom room = matchingService.getRoomContainsUser(user);

            // 패배 브로드캐스트
            WebSocketHandler.CommandMessage command = WebSocketHandler.CommandMessage.builder()
                    .command("game/game_set")
                    .data("LOSE-" + user.getEmail())
                    .build();
            WebSocketUtils.sendCommand(room.getSessions(), command);

            // 방에 속한 모든 플레이어를 매칭에서 제거
            room.getPlayerStatus().forEach(p -> {
                User u = userRepository.findByEmail(p.getUserEmail())
                        .orElseThrow(() -> new IllegalArgumentException("User not found: " + p.getUserEmail()));
                matchingService.leaveRoom(u);
            });
        } catch (IllegalArgumentException e) {
            // 방이 없으면 무시
            System.out.println("플레이어가 존재한 방이 없어 무시되었습니다.");
        }
    }

    public void nextPrice(MatchRoom room) {
        int difference = basePrice - room.getStockPrice();
        int maxChange = Math.max(2, Math.abs(difference));
        int change;

        if (new Random().nextInt(100) < 1) {
            change = new Random().nextInt(-10, 11);
            room.setStockPrice(room.getStockPrice() + change);
            room.setStockPrice(Math.max(room.getStockPrice(), 1));
        } else {
            int minPrice = 10;
            int maxPrice = 30;
            float probability = Math.max(0.2f, 1f - Math.abs(difference) / (float) (maxPrice - minPrice));

            if (new Random().nextFloat() < probability) {
                if (difference > 0) change = new Random().nextInt(-maxChange / 2, maxChange + 1);
                else if (difference < 0) change = new Random().nextInt(-maxChange, maxChange / 2 + 1);
                else change = new Random().nextInt(-3, 4);
            } else {
                change = new Random().nextInt(-1, 2);
            }

            room.setStockPrice(room.getStockPrice() + change);
            room.setStockPrice(Math.min(Math.max(room.getStockPrice(), minPrice), maxPrice));
        }

        room.getPriceHistory().addFirst(room.getStockPrice());
        while (room.getPriceHistory().size() > 50) room.getPriceHistory().removeLast();
    }

    public GameData getGameData(User user) {
        return user.getGameData();
    }

    public void updateGameData(User user, GameData data) {
        userRepository.save(user.update(data));
    }

    public GameData resetGameData(User user) {
        return userRepository.save(user.update(
                GameData.builder()
                        .userEmail(user.getEmail())
                        .currentFunds(100)
                        .goStack(3)
                        .cooldown(0)
                        .stockPrice(20)
                        .priceHistory(new ArrayList<>())
                        .colCooldown(5)
                        .build())).getGameData();
    }
}

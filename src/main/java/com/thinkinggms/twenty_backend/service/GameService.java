package com.thinkinggms.twenty_backend.service;

import com.thinkinggms.twenty_backend.component.WebSocketHandler;
import com.thinkinggms.twenty_backend.statics.MatchRoom;
import com.thinkinggms.twenty_backend.domain.User;
import com.thinkinggms.twenty_backend.dto.StockResponse;
import com.thinkinggms.twenty_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public List<MatchRoom.PlayerStatus> getPlayerStatus(User user) {
        MatchRoom room = matchingService.getRoomContainsUser(user);
        return room.getPlayerStatus().stream().sorted(Comparator.comparingLong(ps -> Math.abs(ps.getUserEmail().compareTo(user.getEmail())))).toList();
    }

    public StockResponse getStockStatus(User user) {
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
        room.getPriceHistory().addFirst((float) room.getStockPrice());
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

    public void onNextTurn(MatchRoom room, WebSocketHandler webSocketHandler) {
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
            webSocketHandler.sendCommand(room.getSessions(),
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
            webSocketHandler.sendCommand(room.getSessions(),
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
            webSocketHandler.sendCommand(room.getSessions(),
                    WebSocketHandler.CommandMessage.builder()
                            .command("game/game_set")
                            .data("LOSE-" + loserUser.getEmail())
                            .build()
            );
            room.getPlayerStatus().forEach(ps -> matchingService.leaveRoom(userRepository.findByEmail(ps.getUserEmail()).orElseThrow()));
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

        room.getPriceHistory().addFirst((float) room.getStockPrice());
        while (room.getPriceHistory().size() > 50) room.getPriceHistory().removeLast();
    }
}

package com.thinkinggms.twenty_backend.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkinggms.twenty_backend.repository.UserRepository;
import com.thinkinggms.twenty_backend.statics.MatchRoom;
import com.thinkinggms.twenty_backend.domain.User;
import com.thinkinggms.twenty_backend.dto.RoomResponse;
import com.thinkinggms.twenty_backend.service.MatchingService;
import com.thinkinggms.twenty_backend.service.GameService;
import com.thinkinggms.twenty_backend.utils.WebSocketUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {
    private final MatchingService matchingService;
    private final UserRepository userRepository;
    private final GameService gameService;
    private final ObjectMapper objectMapper;
    private final ServerScheduler serverScheduler;

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        CommandMessage command = objectMapper.readValue(payload, CommandMessage.class);
        User user = (User) session.getAttributes().get("userPrincipal");
        if (user == null) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                    CommandMessage.builder()
                            .command("error")
                            .data("You are not logged in!")
                            .build()
            )));
            return;
        }
        System.out.println("Message Received: " + payload);
        switch (command.getCommand()) {
            case "match" -> {
                MatchRoom room = matchingService.match(user);
                MatchRoom.PlayerStatus ps = MatchRoom.PlayerStatus.builder()
                        .userEmail(user.getEmail())
                        .currentFunds(100)
                        .isReady(false)
                        .goStack(3)
                        .cooldown(0)
                        .build();
                room.getPlayerStatus().add(ps);
                room.getSessions().add(session);
                if (room.getStatus().equals(MatchRoom.RoomStatus.FULL)) {
                    gameService.initializeRoom(room);
                    WebSocketUtils.sendToEachSocket(room.getSessions(), new TextMessage(objectMapper.writeValueAsString(
                            CommandMessage.builder()
                                    .command("match/matched")
                                    .data("").build()
                    )));
                }
            }
            case "match/leave" -> {
                MatchRoom room = matchingService.getRoomContainsUser(user);
                MatchRoom.PlayerStatus ps = gameService.getPlayerStatus(user).get(0);
                matchingService.leaveRoom(user);
                room.getSessions().remove(session);
                room.getPlayerStatus().remove(ps);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(CommandMessage.builder().command("match/closed").data("").build())));
            }
            case "game/wait_turn" -> {
                MatchRoom room = matchingService.getRoomContainsUser(user);
                MatchRoom.PlayerStatus ps = gameService.getPlayerStatus(user).get(0);
                ps.setIsReady(true);
                if (room.getPlayerStatus().stream().allMatch(MatchRoom.PlayerStatus::getIsReady)) {
                    gameService.onNextTurn(room);
                    WebSocketUtils.sendToEachSocket(room.getSessions(), new TextMessage(objectMapper.writeValueAsString(
                            CommandMessage.builder()
                                    .command("game/turn_changed")
                                    .data(objectMapper.writeValueAsString(RoomResponse.from(room))).build()
                    )));
                }
            }
            case "game/buy_stock" -> {
                MatchRoom room = matchingService.getRoomContainsUser(user);
                MatchRoom.PlayerStatus ps = gameService.getPlayerStatus(user).get(0);
                ps.setIsReady(true);
                gameService.buyStock(room, user, Integer.parseInt(command.getData()));
                if (room.getPlayerStatus().stream().allMatch(MatchRoom.PlayerStatus::getIsReady)) {
                    gameService.onNextTurn(room);
                    WebSocketUtils.sendToEachSocket(room.getSessions(), new TextMessage(objectMapper.writeValueAsString(
                            CommandMessage.builder()
                                    .command("game/turn_changed")
                                    .data(objectMapper.writeValueAsString(RoomResponse.from(room))).build()
                    )));
                }
            }
            case "game/sell_stock" -> {
                MatchRoom room = matchingService.getRoomContainsUser(user);
                MatchRoom.PlayerStatus ps = gameService.getPlayerStatus(user).get(0);
                ps.setIsReady(true);
                gameService.sellStock(room, user, Integer.parseInt(command.getData()));
                if (room.getPlayerStatus().stream().allMatch(MatchRoom.PlayerStatus::getIsReady)) {
                    gameService.onNextTurn(room);
                    WebSocketUtils.sendToEachSocket(room.getSessions(), new TextMessage(objectMapper.writeValueAsString(
                            CommandMessage.builder()
                                    .command("game/turn_changed")
                                    .data(objectMapper.writeValueAsString(RoomResponse.from(room))).build()
                    )));
                }
            }
            case "game/lose" -> {
                try {
                    MatchRoom room = matchingService.getRoomContainsUser(user);
                    WebSocketUtils.sendCommand(room.getSessions(),
                            WebSocketHandler.CommandMessage.builder()
                                    .command("game/game_set")
                                    .data("LOSE-" + user.getEmail())
                                    .build()
                    );
                    room.getPlayerStatus().forEach(p -> matchingService.leaveRoom(userRepository.findByEmail(p.getUserEmail()).orElseThrow()));
                } catch (IllegalArgumentException e) {
                    System.out.println("플레이어가 존재한 방이 없어 무시되었습니다.");
                }
            }
            case "pong" -> serverScheduler.updateSession(session);
            default -> {
            }
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        User user = (User) session.getAttributes().get("userPrincipal");
        if (user == null) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                    CommandMessage.builder()
                            .command("error")
                            .data("You are not logged in!")
                            .build()
            )));
            return;
        }
        serverScheduler.updateSession(session);
        Logger.getLogger(WebSocketHandler.class.getName()).log(Level.INFO, "connection established: " + user.getEmail());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        User user = (User) session.getAttributes().get("userPrincipal");
        Logger.getLogger(WebSocketHandler.class.getName()).log(Level.INFO, "connection closed: " + status);
        if (user == null) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                    CommandMessage.builder()
                            .command("error")
                            .data("You are not logged in!")
                            .build()
            )));
            return;
        }
        Logger.getLogger(WebSocketHandler.class.getName()).log(Level.INFO, user.getEmail() + " left the game.");
        MatchRoom room = matchingService.getRoomContainsUser(user);
        MatchRoom.PlayerStatus ps = gameService.getPlayerStatus(user).get(0);
        matchingService.leaveRoom(user);
        room.getSessions().remove(session);
        room.getPlayerStatus().remove(ps);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(CommandMessage.builder().command("match/closed").data("").build())));
        serverScheduler.removeSession(session);
    }

    @Getter
    @Builder
    public static class CommandMessage {
        private String command;
        private String data;
    }
}

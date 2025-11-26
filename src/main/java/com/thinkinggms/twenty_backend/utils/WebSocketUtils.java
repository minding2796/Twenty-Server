package com.thinkinggms.twenty_backend.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkinggms.twenty_backend.component.WebSocketHandler;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class WebSocketUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void sendCommand(Collection<WebSocketSession> sessions, WebSocketHandler.CommandMessage command) {
        try {
            sendToEachSocket(sessions, new TextMessage(objectMapper.writeValueAsString(command)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendToEachSocket(Collection<WebSocketSession> sessions, TextMessage message) {
        System.out.println("Message Sent: " + message.getPayload());
        sessions.forEach(roomSession -> {
            try {
                roomSession.sendMessage(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}

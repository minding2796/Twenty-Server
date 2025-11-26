package com.thinkinggms.twenty_backend.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkinggms.twenty_backend.service.GameService;
import com.thinkinggms.twenty_backend.utils.WebSocketUtils;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;

@Component
@RequiredArgsConstructor
public class ServerScheduler extends Thread {
    private final ObjectMapper objectMapper;
    private final GameService gameService;
    private boolean enabled = false;
    private final Logger LOGGER = Logger.getLogger(ServerScheduler.class.getSimpleName());
    private final HashMap<WebSocketSession, Long> sessions = new HashMap<>();

    public synchronized void start(WebSocketHandler handler) {
        super.start();
        enabled = true;
    }

    @Override
    public synchronized void start() {
        start(null);
    }

    @Override
    public void interrupt() {
        super.interrupt();
        enabled = false;
    }

    @Override
    public void run() {
        super.run();
        while (enabled) {
            try {
                sleep(1000L);
                WebSocketUtils.sendCommand(sessions.keySet(), WebSocketHandler.CommandMessage.builder().command("ping").build());
                sessions.forEach((session, time) -> {
                    if (System.currentTimeMillis() - time > 10000L) {
                        gameService.lose(session);
                        removeSession(session);
                        try {
                            session.close();
                        } catch (IOException ignored) {
                        }
                    }
                });
            } catch (InterruptedException e) {
                LOGGER.warning(e.getLocalizedMessage());
            }
        }
    }

    public void updateSession(WebSocketSession session) {
        sessions.put(session, System.currentTimeMillis());
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
    }
}

package com.thinkinggms.twenty_backend.config;

import com.thinkinggms.twenty_backend.component.CustomHandshakeInterceptor;
import com.thinkinggms.twenty_backend.component.WebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@RequiredArgsConstructor
@EnableWebSocket
@Configuration
public class WebSocketConfig implements WebSocketConfigurer {
    private final WebSocketHandler webSocketHandler;
    private final CustomHandshakeInterceptor customHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws").setAllowedOriginPatterns("*").addInterceptors(customHandshakeInterceptor);
    }
}
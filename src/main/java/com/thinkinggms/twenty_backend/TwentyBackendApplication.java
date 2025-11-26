package com.thinkinggms.twenty_backend;

import com.thinkinggms.twenty_backend.component.ServerScheduler;
import com.thinkinggms.twenty_backend.component.WebSocketHandler;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@RequiredArgsConstructor
public class TwentyBackendApplication {
    private final ServerScheduler serverScheduler;
    private final WebSocketHandler webSocketHandler;
    
    public static void main(String[] args) {
        // .env 파일 로드
        try {
            Dotenv dotenv = Dotenv.configure().load();
            dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
        } catch (DotenvException ignored) {
        }

        SpringApplication.run(TwentyBackendApplication.class, args);
    }

    public void init(ApplicationArguments args) {
        serverScheduler.start(webSocketHandler);
    }

    @Bean
    public ApplicationRunner applicationRunner() {
        return this::init;
    }
}

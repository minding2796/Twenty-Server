package com.thinkinggms.twenty_backend;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TwentyBackendApplication {
    public static void main(String[] args) {
        // .env 파일 로드
        try {
            Dotenv dotenv = Dotenv.configure().load();
            dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
        } catch (DotenvException ignored) {
        }

        SpringApplication.run(TwentyBackendApplication.class, args);
    }
}

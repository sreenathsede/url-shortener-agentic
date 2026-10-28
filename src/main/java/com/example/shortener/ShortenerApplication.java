package com.example.shortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
/**
 * Bootstraps the URL shortener and its Spring-managed components.
 */
public class ShortenerApplication {
    /**
     * Starts the Spring Boot application.
     */
    public static void main(String[] args) {
        SpringApplication.run(ShortenerApplication.class, args);
    }
}

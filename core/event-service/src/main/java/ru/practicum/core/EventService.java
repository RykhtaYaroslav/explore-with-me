package ru.practicum.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"ru.practicum.core", "ru.practicum.stats.client"})
@EnableFeignClients(basePackages = {"ru.practicum.core", "ru.practicum.stats.client"})
public class EventService {
    public static void main(String[] args) {
        SpringApplication.run(EventService.class, args);
    }
}

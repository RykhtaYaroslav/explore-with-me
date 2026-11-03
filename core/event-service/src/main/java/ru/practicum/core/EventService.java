package ru.practicum.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import ru.practicum.core.common.exception.CommonErrorHandler;
import ru.practicum.core.common.logging.RequestLoggingConfiguration;

@SpringBootApplication(scanBasePackages = {"ru.practicum.core", "ru.practicum.stats.client"})
@Import({CommonErrorHandler.class, RequestLoggingConfiguration.class})
@EnableFeignClients(basePackages = {"ru.practicum.core", "ru.practicum.stats.client"})
public class EventService {
    public static void main(String[] args) {
        SpringApplication.run(EventService.class, args);
    }
}

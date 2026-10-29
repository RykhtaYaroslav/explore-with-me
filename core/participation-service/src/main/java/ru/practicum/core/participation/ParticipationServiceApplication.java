package ru.practicum.core.participation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import ru.practicum.core.common.exception.CommonErrorHandler;

@SpringBootApplication
@EnableFeignClients(basePackages = {
        "ru.practicum.core.participation.client",
        "ru.practicum.core.participation.interservice.feign"
})
@Import(CommonErrorHandler.class)
public class ParticipationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ParticipationServiceApplication.class, args);
    }
}

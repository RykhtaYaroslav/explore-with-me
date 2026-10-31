package ru.practicum.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import ru.practicum.core.common.exception.CommonErrorHandler;
import ru.practicum.core.common.time.TimeConfig;
import ru.practicum.main.request.client.EventFeignClient;
import ru.practicum.stats.client.FeignStatsClient;

@SpringBootApplication
@ComponentScan(basePackages = {"ru.practicum.main", "ru.practicum.stats.client"})
@EntityScan(basePackages = {"ru.practicum.main", "ru.practicum.core.user.model"})
@EnableJpaRepositories(basePackages = {"ru.practicum.main", "ru.practicum.core.user.repository"})
@EnableFeignClients(clients = {FeignStatsClient.class, EventFeignClient.class})
@Import({CommonErrorHandler.class, TimeConfig.class})
public class MainApplication {
    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}

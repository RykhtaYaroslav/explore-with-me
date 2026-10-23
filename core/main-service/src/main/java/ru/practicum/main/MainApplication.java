package ru.practicum.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import ru.practicum.stats.client.FeignStatsClient;

@SpringBootApplication
@ComponentScan(basePackages = {"ru.practicum.main", "ru.practicum.stats.client"})
@EnableFeignClients(clients = FeignStatsClient.class)
public class MainApplication {
    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}

package ru.practicum.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import ru.practicum.core.common.exception.CommonErrorHandler;
import ru.practicum.core.common.logging.RequestLoggingConfiguration;
import ru.practicum.stats.client.grpc.AnalyzerGrpcClient;
import ru.practicum.stats.client.grpc.CollectorGrpcClient;

@SpringBootApplication
@Import({CommonErrorHandler.class, RequestLoggingConfiguration.class, CollectorGrpcClient.class, AnalyzerGrpcClient.class})
@EnableFeignClients
public class EventService {
    public static void main(String[] args) {
        SpringApplication.run(EventService.class, args);
    }
}

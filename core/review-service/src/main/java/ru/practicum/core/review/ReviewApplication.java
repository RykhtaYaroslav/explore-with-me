package ru.practicum.core.review;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import ru.practicum.core.common.exception.CommonErrorHandler;
import ru.practicum.core.common.logging.RequestLoggingConfiguration;
import ru.practicum.core.common.time.TimeConfig;
import ru.practicum.stats.client.grpc.CollectorGrpcClient;

@SpringBootApplication
@EnableFeignClients(basePackages = "ru.practicum.core.review.interservice.feign")
@Import({CommonErrorHandler.class, RequestLoggingConfiguration.class, TimeConfig.class, CollectorGrpcClient.class})
public class ReviewApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReviewApplication.class, args);
    }
}

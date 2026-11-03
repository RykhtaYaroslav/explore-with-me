package ru.practicum.core.compilation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import ru.practicum.core.common.exception.CommonErrorHandler;
import ru.practicum.core.common.logging.RequestLoggingConfiguration;
import ru.practicum.core.compilation.interservice.feign.EventFeignClient;

@SpringBootApplication
@EnableFeignClients(clients = EventFeignClient.class)
@Import({CommonErrorHandler.class, RequestLoggingConfiguration.class})
public class CompilationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CompilationServiceApplication.class, args);
    }
}

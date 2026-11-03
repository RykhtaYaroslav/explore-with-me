package ru.practicum.stats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import ru.practicum.core.common.logging.RequestLoggingConfiguration;

@SpringBootApplication
@Import(RequestLoggingConfiguration.class)
public class StatsServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(StatsServerApplication.class, args);
    }
}

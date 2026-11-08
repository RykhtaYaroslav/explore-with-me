package ru.practicum.ewm.stats.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "ru.practicum.ewm.stats")
@EnableConfigurationProperties(RecommendationProperties.class)
public class AnalyzerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnalyzerApplication.class, args);
    }
}

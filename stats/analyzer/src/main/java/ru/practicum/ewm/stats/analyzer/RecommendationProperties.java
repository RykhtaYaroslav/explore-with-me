package ru.practicum.ewm.stats.analyzer;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("stats.recommendations")
public record RecommendationProperties(
        @DefaultValue("20") int recentEvents,
        @DefaultValue("5") int neighbors
) {
    public RecommendationProperties {
        if (recentEvents <= 0 || neighbors <= 0) {
            throw new IllegalArgumentException("Recent events and neighbors must be positive");
        }
    }
}

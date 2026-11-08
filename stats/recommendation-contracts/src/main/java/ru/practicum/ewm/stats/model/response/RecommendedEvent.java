package ru.practicum.ewm.stats.model.response;

import lombok.Builder;

@Builder
public record RecommendedEvent(
        long eventId,
        double score
) {
}

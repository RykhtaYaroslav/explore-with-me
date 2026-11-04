package ru.practicum.ewm.stats.model.similarity;

import lombok.Builder;

import java.time.Instant;

@Builder
public record EventSimilarity(
        long eventA,
        long eventB,
        double score,
        Instant timestamp
) {
}

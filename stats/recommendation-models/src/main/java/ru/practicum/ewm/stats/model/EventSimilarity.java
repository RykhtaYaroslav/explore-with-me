package ru.practicum.ewm.stats.model;

import java.time.Instant;

public record EventSimilarity(
        long eventA,
        long eventB,
        double score,
        Instant timestamp
) {
}

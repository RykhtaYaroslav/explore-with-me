package ru.practicum.ewm.stats.model.request;

import lombok.Builder;

@Builder
public record SimilarEventsRequest(
        long eventId,
        long userId,
        int maxResults
) {
}

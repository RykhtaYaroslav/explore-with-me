package ru.practicum.ewm.stats.model.request;

import lombok.Builder;

@Builder
public record UserPredictionsRequest(
        long userId,
        int maxResults
) {
}

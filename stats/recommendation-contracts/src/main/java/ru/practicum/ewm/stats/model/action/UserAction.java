package ru.practicum.ewm.stats.model.action;

import lombok.Builder;

import java.time.Instant;

@Builder
public record UserAction(
        long userId,
        long eventId,
        ActionType actionType,
        Instant timestamp
) {
}

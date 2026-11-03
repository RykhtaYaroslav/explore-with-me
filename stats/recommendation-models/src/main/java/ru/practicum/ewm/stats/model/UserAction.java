package ru.practicum.ewm.stats.model;

import java.time.Instant;

public record UserAction(
        long userId,
        long eventId,
        ActionType actionType,
        Instant timestamp
) {
}

package ru.practicum.ewm.stats.model.request;

import lombok.Builder;
import lombok.Singular;

import java.util.List;

@Builder
public record InteractionsCountRequest(
        @Singular("eventId") List<Long> eventIds
) {
    public InteractionsCountRequest {
        eventIds = List.copyOf(eventIds);
    }
}

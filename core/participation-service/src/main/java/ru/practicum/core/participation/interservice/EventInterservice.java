package ru.practicum.core.participation.interservice;

import ru.practicum.core.event.dto.response.EventForParticipationServiceDto;

public interface EventInterservice {
    void ensureOrganizerOrThrow(Long eventId, Long userId);

    EventForParticipationServiceDto findByIdForOrganizerOrThrow(Long eventId, Long userId);

    EventForParticipationServiceDto findByIdOrThrow(Long eventId);

    void synchronizeConfirmedRequestsOrThrow(Long eventId, long expectedCount, long count);
}

package ru.practicum.core.participation.interservice;

import ru.practicum.core.event.dto.response.EventDtoForParticipationService;

public interface EventInterservice {
    void ensureOrganizerOrThrow(Long eventId, Long userId);

    EventDtoForParticipationService findByIdForOrganizerOrThrow(Long eventId, Long userId);

    EventDtoForParticipationService findByIdOrThrow(Long eventId);

    void synchronizeConfirmedRequestsOrThrow(Long eventId, long expectedCount, long count);
}

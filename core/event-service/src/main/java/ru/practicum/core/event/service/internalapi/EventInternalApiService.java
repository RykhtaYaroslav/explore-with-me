package ru.practicum.core.event.service.internalapi;

import ru.practicum.core.event.dto.response.EventForParticipationServiceDto;
import ru.practicum.core.event.dto.response.EventShortDto;

import java.util.List;

public interface EventInternalApiService {
    boolean existsById(Long eventId);

    boolean isOrganizer(Long eventId, Long userId);

    EventShortDto getReviewInfo(Long eventId);

    List<EventShortDto> getCompilationInfo(List<Long> eventIds);

    EventForParticipationServiceDto getParticipationInfoOrThrow(Long eventId);

    void adjustCount(Long eventId, long delta);

    void synchronizeConfirmedRequests(Long eventId, long expectedCount, long count);
}

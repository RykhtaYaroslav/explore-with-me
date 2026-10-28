package ru.practicum.core.event.service.interservice;

import ru.practicum.core.event.dto.response.EventShortDto;

public interface EventInternalService {
    boolean existsById(Long eventId);

    boolean isOrganizer(Long eventId, Long userId);

    EventShortDto getReviewInfo(Long eventId);
}

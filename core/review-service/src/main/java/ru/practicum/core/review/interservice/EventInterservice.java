package ru.practicum.core.review.interservice;

import ru.practicum.core.event.dto.response.EventShortDto;

import java.util.List;

public interface EventInterservice {
    boolean existsById(Long eventId);

    boolean isOrganizer(Long eventId, Long userId);

    EventShortDto getReviewInfo(Long eventId);

    List<EventShortDto> getReviewInfoByIds(List<Long> eventIds);
}

package ru.practicum.core.event.service.publicapi;

import ru.practicum.core.event.dto.EventQueryParams;
import ru.practicum.core.event.dto.response.EventFullDto;
import ru.practicum.core.event.dto.response.EventShortDto;

import java.util.List;

public interface EventServicePublic {
    List<EventShortDto> getAllWithParams(EventQueryParams params);

    /** Returns a published event and records the view when the supplied user ID exists. */
    EventFullDto getEventFullInformation(Long id, Long userId);
}

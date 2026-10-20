package ru.practicum.main.event.service.publicapi;

import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.EventQueryParams;
import ru.practicum.main.event.dto.EventShortDto;

import java.util.List;

public interface EventServicePublic {
    List<EventShortDto> getAllWithParams(EventQueryParams params);

    EventFullDto getEventFullInformation(Long id);

    void hitStat(String uri, String ip);
}

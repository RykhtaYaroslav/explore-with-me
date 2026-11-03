package ru.practicum.core.compilation.interservice;

import ru.practicum.core.event.dto.response.EventShortDto;

import java.util.List;

public interface EventInterservice {
    List<EventShortDto> findByIds(List<Long> eventIds);
}

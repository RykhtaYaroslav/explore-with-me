package ru.practicum.main.event.service;

import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.model.Event;

import java.util.List;
import java.util.Map;

public interface EventStatsCollector {
    List<EventShortDto> getShortDtoListWithStats(List<Event> events);

    List<EventFullDto> getFullDtoListWithStats(List<Event> events);

    Map<Long, Long> getConReqByEventMap(List<Event> events);

    Map<Long, Long> getViewsByEventMap(List<Event> events);
}

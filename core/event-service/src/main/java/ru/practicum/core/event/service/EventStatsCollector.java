package ru.practicum.core.event.service;

import ru.practicum.core.event.dto.response.EventFullDto;
import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.core.event.model.Event;

import java.util.List;
import java.util.Map;

public interface EventStatsCollector {
    List<EventShortDto> getShortDtoListWithStats(List<Event> events);

    List<EventFullDto> getFullDtoListWithStats(List<Event> events);

    Map<Long, Long> getConReqByEventMap(List<Event> events);

    Map<Long, Long> getViewsByEventMap(List<Event> events);

    /** Records a view in the existing event view counter. */
    void recordView(long eventId, String ip);

    /** Returns interaction weights keyed by event ID. */
    Map<Long, Double> getInteractionWeights(List<Long> eventIds);
}

package ru.practicum.core.event.service.privateapi;

import ru.practicum.core.event.dto.request.NewEventDto;
import ru.practicum.core.event.dto.request.UpdateEventUserRequest;
import ru.practicum.core.event.dto.response.EventFullDto;
import ru.practicum.core.event.dto.response.EventShortDto;

import java.util.List;

public interface EventServicePrivate {
    EventFullDto create(Long userId, NewEventDto newEventDto);

    List<EventShortDto> findAllByInitiatorId(Long userId, Integer from, Integer size);

    EventFullDto findByInitiatorAndEventIds(Long userId, Long eventId);

    EventFullDto updateEventByInitiator(UpdateEventUserRequest request, Long userId, Long eventId);
}

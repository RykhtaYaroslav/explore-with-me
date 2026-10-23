package ru.practicum.main.event.service.privateapi;

import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.EventRequestStatusUpdateRequest;
import ru.practicum.main.event.dto.EventRequestStatusUpdateResult;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.dto.NewEventDto;
import ru.practicum.main.event.dto.UpdateEventUserRequest;
import ru.practicum.main.request.dto.ParticipationRequestDto;

import java.util.List;

public interface EventServicePrivate {
    EventFullDto create(Long userId, NewEventDto newEventDto);

    List<EventShortDto> findAllByInitiatorId(Long userId, Integer from, Integer size);

    EventFullDto findByInitiatorAndEventIds(Long userId, Long eventId);

    EventFullDto updateEventByInitiator(UpdateEventUserRequest request, Long userId, Long eventId);

    List<ParticipationRequestDto> getEventRequestsByInitiator(Long userId, Long eventId);

    EventRequestStatusUpdateResult changeRequestsStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest request);
}

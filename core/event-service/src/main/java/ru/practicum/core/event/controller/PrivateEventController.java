package ru.practicum.core.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.core.event.dto.request.NewEventDto;
import ru.practicum.core.event.dto.request.UpdateEventUserRequest;
import ru.practicum.core.event.dto.response.EventFullDto;
import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.core.event.service.privateapi.EventServicePrivate;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
public class PrivateEventController {
    private static final String USER_EVENTS = "/users/{userId}/events";
    private final EventServicePrivate eventServicePrivate;

    /** Returns up to `size` published recommendations for the registered user. */
    @GetMapping("/events/recommendations")
    public List<EventShortDto> getRecommendations(
            @RequestHeader("X-EWM-USER-ID") @Positive Long userId,
            @RequestParam(defaultValue = "10") @Positive int size) {
        return eventServicePrivate.getRecommendations(userId, size);
    }

    @PostMapping(USER_EVENTS)
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto create(@RequestBody @Valid NewEventDto newEventDto, @PathVariable @Positive Long userId) {
        return eventServicePrivate.create(userId, newEventDto);
    }

    @GetMapping(USER_EVENTS)
    public List<EventShortDto> findAllByInitiatorId(
            @PathVariable @Positive Long userId,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size) {
        return eventServicePrivate.findAllByInitiatorId(userId, from, size);
    }

    @GetMapping(USER_EVENTS + "/{eventId}")
    public EventFullDto findByInitiatorAndEventIds(@PathVariable @Positive Long userId, @PathVariable @Positive Long eventId) {
        return eventServicePrivate.findByInitiatorAndEventIds(userId, eventId);

    }

    @PatchMapping(USER_EVENTS + "/{eventId}")
    public EventFullDto updateEventByInitiator(@RequestBody @Valid UpdateEventUserRequest request, @PathVariable @Positive Long userId, @PathVariable @Positive Long eventId) {
        return eventServicePrivate.updateEventByInitiator(request, userId, eventId);
    }
}

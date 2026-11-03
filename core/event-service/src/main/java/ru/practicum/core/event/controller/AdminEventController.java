package ru.practicum.core.event.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.core.common.exception.BadRequestException;
import ru.practicum.core.event.dto.EventQueryParams;
import ru.practicum.core.event.dto.request.UpdateEventAdminRequest;
import ru.practicum.core.event.dto.response.EventFullDto;
import ru.practicum.core.event.model.EventState;
import ru.practicum.core.event.service.adminapi.EventServiceAdmin;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.core.common.util.EwmConstants.DATE_TIME_FORMAT;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
@Validated
public class AdminEventController {
    private final EventServiceAdmin eventServiceAdmin;

    @GetMapping
    public List<EventFullDto> getAllFullInfoWithParams(
            @RequestParam(required = false) List<@Positive(message = "Users ids should be positive") Long> users,
            @RequestParam(required = false) List<EventState> states,
            @RequestParam(required = false) List<@Positive(message = "Categories ids should be positive") Long> categories,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") @PositiveOrZero(message = "Parameter \"from\" should be positive or zero") Integer from,
            @RequestParam(defaultValue = "10") @Positive(message = "Query parameter \"size\" must be positive") Integer size) {

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("Start date cannot be after end date");
        }

        users = CollectionUtils.isEmpty(users) ? null : users;
        states = CollectionUtils.isEmpty(states) ? null : states;
        categories = CollectionUtils.isEmpty(categories) ? null : categories;

        EventQueryParams params = EventQueryParams.ofAdmin(
                users,
                states,
                categories,
                rangeStart,
                rangeEnd,
                from,
                size);

        return eventServiceAdmin.getAllFullInfoWithParams(params);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(@PathVariable @Positive(message = "Event id should be positive") Long eventId,
                                    @RequestBody @Valid UpdateEventAdminRequest request) {
        return eventServiceAdmin.updateEvent(eventId, request);
    }
}

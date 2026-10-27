package ru.practicum.core.event.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.core.common.exception.BadRequestException;
import ru.practicum.core.event.dto.EventQueryParams;
import ru.practicum.core.event.dto.request.EventSort;
import ru.practicum.core.event.dto.response.EventFullDto;
import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.core.event.service.publicapi.EventServicePublic;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.core.common.util.EwmConstants.DATE_TIME_FORMAT;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Validated
public class PublicEventController {
    private final EventServicePublic eventServicePublic;

    @GetMapping
    public List<EventShortDto> getAllWithParams(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<@Positive(message = "Category ID must be positive") Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = DATE_TIME_FORMAT) LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) EventSort sort,
            @RequestParam(defaultValue = "0") @PositiveOrZero(message = "Query parameter \"from\" must be positive or zero") Integer from,
            @RequestParam(defaultValue = "10") @Positive(message = "Query parameter \"size\" must be positive") Integer size,
            HttpServletRequest request) {
        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }

        if (rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("Start date cannot be after end date");
        }

        EventQueryParams params = EventQueryParams.ofPublic(
                text,
                categories,
                paid,
                rangeStart,
                rangeEnd,
                onlyAvailable,
                sort,
                from,
                size
        );

        List<EventShortDto> result = eventServicePublic.getAllWithParams(params);

        eventServicePublic.hitStat(request.getRequestURI(), request.getRemoteAddr());
        return result;
    }

    @GetMapping("/{id}")
    public EventFullDto getEventFullInformation(@PathVariable @Positive Long id, HttpServletRequest request) {
        EventFullDto result = eventServicePublic.getEventFullInformation(id);

        eventServicePublic.hitStat(request.getRequestURI(), request.getRemoteAddr());
        return result;
    }
}

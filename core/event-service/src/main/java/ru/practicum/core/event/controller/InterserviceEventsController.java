package ru.practicum.core.event.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.core.event.service.EventConfirmedRequestsService;
import ru.practicum.core.event.service.interservice.EventInternalService;

@Validated
@RestController
@RequestMapping("/interservice/events")
@RequiredArgsConstructor
public class InterserviceEventsController {
    private final EventConfirmedRequestsService eventConfirmedRequestsService;
    private final EventInternalService eventInternalService;

    @GetMapping("/exist/{eventId}")
    public boolean existsById(@PathVariable @Positive Long eventId) {
        return eventInternalService.existsById(eventId);
    }

    @GetMapping("/{eventId}/organizers/{userId}")
    public boolean isOrganizer(@PathVariable @Positive Long eventId,
                               @PathVariable @Positive Long userId) {
        return eventInternalService.isOrganizer(eventId, userId);
    }

    @GetMapping("/{eventId}/review-info")
    public EventShortDto getReviewInfo(@PathVariable @Positive Long eventId) {
        return eventInternalService.getReviewInfo(eventId);
    }

    @PatchMapping("/{eventId}/confirmed-requests")
    public void adjustConfirmedRequests(@PathVariable @Positive Long eventId,
                                        @RequestParam long delta) {
        eventConfirmedRequestsService.adjustCount(eventId, delta);
    }
}

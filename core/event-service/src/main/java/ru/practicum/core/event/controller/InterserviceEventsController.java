package ru.practicum.core.event.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.core.event.service.EventConfirmedRequestsService;

@Validated
@RestController
@RequestMapping("/interservice/events")
@RequiredArgsConstructor
public class InterserviceEventsController {
    private final EventConfirmedRequestsService eventConfirmedRequestsService;

    @PatchMapping("/{eventId}/confirmed-requests")
    public void adjustConfirmedRequests(@PathVariable @Positive Long eventId,
                                        @RequestParam long delta) {
        eventConfirmedRequestsService.adjustCount(eventId, delta);
    }
}

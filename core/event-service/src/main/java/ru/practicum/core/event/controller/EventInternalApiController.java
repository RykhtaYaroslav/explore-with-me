package ru.practicum.core.event.controller;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.core.event.dto.response.EventForParticipationServiceDto;
import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.core.event.service.internalapi.EventInternalApiService;

import java.util.List;

@Validated
@RestController
@RequestMapping("/interservice/events")
@RequiredArgsConstructor
public class EventInternalApiController {
    private final EventInternalApiService eventInternalApiService;

    @GetMapping("/exist/{eventId}")
    public boolean existsById(@PathVariable @Positive Long eventId) {
        return eventInternalApiService.existsById(eventId);
    }

    @GetMapping("/{eventId}/organizers/{userId}")
    public boolean isOrganizer(@PathVariable @Positive Long eventId,
                               @PathVariable @Positive Long userId) {
        return eventInternalApiService.isOrganizer(eventId, userId);
    }

    @GetMapping("/{eventId}/review-info")
    public EventShortDto getReviewInfo(@PathVariable @Positive Long eventId) {
        return eventInternalApiService.getReviewInfo(eventId);
    }

    @GetMapping("/review-info")
    public List<EventShortDto> getReviewInfoByIds(@RequestParam("ids") List<@Positive Long> eventIds) {
        return eventInternalApiService.getReviewInfoByIds(eventIds);
    }

    @GetMapping("/compilation-info")
    public List<EventShortDto> getCompilationInfo(@RequestParam("ids") List<@Positive Long> eventIds) {
        return eventInternalApiService.getCompilationInfo(eventIds);
    }

    @GetMapping("/{eventId}/participation-info")
    public EventForParticipationServiceDto getParticipationInfo(@PathVariable @Positive Long eventId) {
        return eventInternalApiService.getParticipationInfoOrThrow(eventId);
    }

    @PatchMapping("/{eventId}/confirmed-requests")
    public void adjustConfirmedRequests(@PathVariable @Positive Long eventId,
                                        @RequestParam long delta) {
        eventInternalApiService.adjustCount(eventId, delta);
    }

    @PutMapping("/{eventId}/confirmed-requests")
    public void synchronizeConfirmedRequests(@PathVariable @Positive Long eventId,
                                             @RequestParam("expectedCount") @PositiveOrZero long expectedCount,
                                             @RequestParam("count") @PositiveOrZero long count) {
        eventInternalApiService.synchronizeConfirmedRequests(eventId, expectedCount, count);
    }
}

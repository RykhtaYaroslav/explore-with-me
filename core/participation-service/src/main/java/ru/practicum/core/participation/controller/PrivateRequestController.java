package ru.practicum.core.participation.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.core.participation.service.RequestService;
import ru.practicum.main.request.dto.ParticipationRequestDto;

import java.util.List;

@Validated
@RestController
@RequestMapping("/users/{userId}/requests")
@RequiredArgsConstructor
public class PrivateRequestController {

    private final RequestService requestService;

    @GetMapping
    public List<ParticipationRequestDto> getUserRequests(@PathVariable @Positive Long userId) {
        return requestService.getUserRequests(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequest(@PathVariable @Positive Long userId,
                                                 @RequestParam @Positive Long eventId) {
        return requestService.createRequest(userId, eventId);
    }

    @PatchMapping("/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable @Positive Long userId,
                                                 @PathVariable @Positive Long requestId) {
        return requestService.cancelRequest(userId, requestId);
    }

//    @GetMapping("/{eventId}/requests")
//    public List<ParticipationRequestDto> getEventRequestsByInitiator(@PathVariable @Positive Long userId, @PathVariable @Positive Long eventId) {
//        return requestService.getEventRequestsByInitiator(userId, eventId);
//    }
//
//    @PatchMapping("/{eventId}/requests")
//    public EventRequestStatusUpdateResult changeRequestsStatus(@PathVariable @Positive Long userId, @PathVariable @Positive Long eventId, @RequestBody EventRequestStatusUpdateRequest request) {
//        return requestService.changeRequestsStatus(userId, eventId, request);
//    }
}

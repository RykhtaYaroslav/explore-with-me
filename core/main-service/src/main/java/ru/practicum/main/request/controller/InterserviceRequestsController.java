package ru.practicum.main.request.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.main.request.service.RequestService;

@RestController
@RequestMapping("/interservice/requests")
@RequiredArgsConstructor
@Validated
public class InterserviceRequestsController {
    private final RequestService requestService;

    @GetMapping("/users/{userId}/events/{eventId}/confirmed")
    public boolean isConfirmedParticipant(@PathVariable @Positive Long userId,
                                          @PathVariable @Positive Long eventId) {
        return requestService.isConfirmedParticipant(userId, eventId);
    }
}

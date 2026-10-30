package ru.practicum.core.participation.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.core.common.enums.RequestStatus;
import ru.practicum.core.common.exception.ConflictException;
import ru.practicum.core.event.dto.response.EventForParticipationServiceDto;
import ru.practicum.core.event.model.EventState;
import ru.practicum.core.participation.interservice.EventInterservice;
import ru.practicum.core.participation.interservice.UserInterservice;
import ru.practicum.core.participation.model.ParticipationRequest;
import ru.practicum.core.participation.repository.RequestRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RequestValidatorImpl implements RequestValidator {
    private static final String OUT_OF_LIMIT_MESSAGE = "There are no available spots left for this event";

    private final UserInterservice userInterservice;
    private final EventInterservice eventInterservice;
    private final RequestRepository requestRepository;

    @Override
    public void validateUserExists(Long userId) {
        userInterservice.existsByIdOrThrow(userId);
    }

    @Override
    public RequestStatus validateCreateRequestAndGetStatus(Long userId, Long eventId) {
        validateUserExists(userId);
        EventForParticipationServiceDto event = eventInterservice.findByIdOrThrow(eventId);

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("You have already requested participation in this event");
        }

        if (event.initiatorId().equals(userId)) {
            throw new ConflictException("The event initiator cannot request participation in their own event");
        }

        if (event.state() != EventState.PUBLISHED) {
            throw new ConflictException("You cannot request participation in an unpublished event");
        }

        if (!event.eventDate().isAfter(LocalDateTime.now())) {
            throw new ConflictException("You cannot request participation in an event that has already started");
        }

        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        Long eventConfirmedCount = event.confirmedRequests();
        if (eventConfirmedCount == null) {
            throw new ConflictException("Event response does not contain confirmed request count");
        }
        if (confirmedCount != eventConfirmedCount) {
            eventInterservice.synchronizeConfirmedRequestsOrThrow(
                    eventId, eventConfirmedCount, confirmedCount);
        }

        int participantLimit = event.participantLimit();
        if (participantLimit > 0 && confirmedCount >= participantLimit) {
            throw new ConflictException(OUT_OF_LIMIT_MESSAGE);
        }

        if (participantLimit == 0 || !event.requestModeration()) {
            return RequestStatus.CONFIRMED;
        }
        return RequestStatus.PENDING;
    }

    @Override
    public void validateCancelRequest(Long userId, ParticipationRequest request) {
        if (!request.getRequesterId().equals(userId)) {
            throw new ConflictException("You cannot cancel another user's participation request");
        }
    }
}

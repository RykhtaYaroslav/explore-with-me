package ru.practicum.core.participation.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.core.common.enums.RequestStatus;
import ru.practicum.core.common.exception.ConflictException;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.event.dto.response.EventForParticipationServiceDto;
import ru.practicum.core.event.model.EventState;
import ru.practicum.core.participation.dto.EventRequestStatusUpdateRequest;
import ru.practicum.core.participation.dto.internal.RequestStatusUpdateContext;
import ru.practicum.core.participation.interservice.EventInterservice;
import ru.practicum.core.participation.interservice.UserInterservice;
import ru.practicum.core.participation.model.ParticipationRequest;
import ru.practicum.core.participation.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Override
    public void validateEventRequestsAccess(Long userId, Long eventId) {
        validateUserExists(userId);
        eventInterservice.ensureOrganizerOrThrow(eventId, userId);
    }

    @Override
    public RequestStatusUpdateContext validateChangeRequestsStatus(Long userId, Long eventId,
                                                                   EventRequestStatusUpdateRequest update) {
        EventForParticipationServiceDto event = eventInterservice.findByIdForOrganizerOrThrow(eventId, userId);
        validateEventRequiresRequestModeration(event);

        RequestStatus targetStatus = validateAndGetTargetStatus(update);
        long confirmedRequestCount = getConfirmedRequestCount(eventId, event);
        validateParticipantLimitNotReached(eventId, event.participantLimit(), confirmedRequestCount);

        Set<Long> requestIds = validateAndGetUniqueRequestIds(update);
        List<ParticipationRequest> requests = findRequestsToUpdate(eventId, requestIds);
        validateRequestsBelongToEventAndArePending(requests, requestIds, eventId);

        return new RequestStatusUpdateContext(targetStatus, requests, confirmedRequestCount,
                event.participantLimit());
    }

    private RequestStatus validateAndGetTargetStatus(EventRequestStatusUpdateRequest update) {
        if (update == null || update.getStatus() == null
                || (update.getStatus() != RequestStatus.CONFIRMED && update.getStatus() != RequestStatus.REJECTED)) {
            throw new ConflictException(String.format("Request status must be %s or %s",
                    RequestStatus.CONFIRMED, RequestStatus.REJECTED));
        }
        return update.getStatus();
    }

    private Set<Long> validateAndGetUniqueRequestIds(EventRequestStatusUpdateRequest update) {
        List<Long> requestIds = update.getRequestsIds();
        if (requestIds == null || requestIds.isEmpty()) {
            return Set.of();
        }
        if (requestIds.contains(null)) {
            throw new ConflictException("Request ids must not contain null values");
        }
        return new HashSet<>(requestIds);
    }

    private List<ParticipationRequest> findRequestsToUpdate(Long eventId, Set<Long> requestIds) {
        if (requestIds.isEmpty()) {
            return requestRepository.findAllByEventIdAndStatus(eventId, RequestStatus.PENDING);
        }

        List<ParticipationRequest> requests = requestRepository.findAllById(requestIds);
        if (requests.size() != requestIds.size()) {
            throw new NotFoundException(String.format("Some requests were not found among IDs: %s", requestIds));
        }
        return requests;
    }

    private void validateRequestsBelongToEventAndArePending(List<ParticipationRequest> requests,
                                                            Set<Long> requestedIds,
                                                            Long eventId) {
        boolean anyRequestIsInvalid = requests.stream()
                .anyMatch(request -> !request.getEventId().equals(eventId)
                        || request.getStatus() != RequestStatus.PENDING);
        if ((!requestedIds.isEmpty() && requests.size() != requestedIds.size()) || anyRequestIsInvalid) {
            throw new ConflictException("All requests must be pending requests for this event");
        }
    }

    private void validateEventRequiresRequestModeration(EventForParticipationServiceDto event) {
        if (event.participantLimit() == 0 || !event.requestModeration()) {
            throw new ConflictException(
                    "Confirmation is not required for events with 0 limit or disabled moderation");
        }
    }

    private long getConfirmedRequestCount(Long eventId, EventForParticipationServiceDto event) {
        long repositoryCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        Long eventCount = event.confirmedRequests();
        if (eventCount == null) {
            throw new ConflictException("Event response does not contain confirmed request count");
        }
        if (repositoryCount != eventCount) {
            eventInterservice.synchronizeConfirmedRequestsOrThrow(eventId, eventCount, repositoryCount);
        }
        return repositoryCount;
    }

    private void validateParticipantLimitNotReached(Long eventId, int participantLimit, long confirmedCount) {
        if (confirmedCount >= participantLimit) {
            throw new ConflictException(String.format(
                    "The participant limit has been reached for event id = %d", eventId));
        }
    }
}

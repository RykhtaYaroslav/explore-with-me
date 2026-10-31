package ru.practicum.core.participation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.core.common.enums.RequestStatus;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.participation.client.EventFeignClient;
import ru.practicum.core.participation.dto.EventRequestStatusUpdateRequest;
import ru.practicum.core.participation.dto.EventRequestStatusUpdateResult;
import ru.practicum.core.participation.dto.ParticipationRequestDto;
import ru.practicum.core.participation.dto.internal.RequestStatusUpdateContext;
import ru.practicum.core.participation.dto.mapper.RequestMapper;
import ru.practicum.core.participation.model.ParticipationRequest;
import ru.practicum.core.participation.repository.RequestRepository;
import ru.practicum.core.participation.validation.RequestValidator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implements participation request operations and synchronizes confirmed request counts with the event service.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final RequestValidator requestValidator;
    private final RequestMapper requestMapper;
    private final EventFeignClient eventFeignClient;

    /**
     * Returns all participation requests submitted by a user.
     *
     * @param userId identifier of the requester
     * @return the user's participation requests
     */
    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        requestValidator.validateUserExists(userId);

        return requestRepository.findAllByRequesterId(userId).stream()
                .map(requestMapper::toDtoOut)
                .collect(Collectors.toList());
    }

    /**
     * Creates a participation request and updates the event's confirmed count when moderation is not required.
     *
     * @param userId identifier of the requester
     * @param eventId identifier of the event
     * @return the created request
     */
    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        RequestStatus startStatus = requestValidator.validateCreateRequestAndGetStatus(userId, eventId);

        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .eventId(eventId)
                .requesterId(userId)
                .status(startStatus)
                .build();

        ParticipationRequest savedRequest = requestRepository.save(request);
        if (startStatus == RequestStatus.CONFIRMED) {
            eventFeignClient.adjustConfirmedRequestCount(eventId, 1);
        }
        return requestMapper.toDtoOut(savedRequest);
    }


    /**
     * Cancels a user's participation request and decrements the event's confirmed count when necessary.
     *
     * @param userId identifier of the requester
     * @param requestId identifier of the request to cancel
     * @return the cancelled request
     */
    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException(String.format("Request with id = %d was not found", requestId)));

        requestValidator.validateCancelRequest(userId, request);

        boolean wasConfirmed = request.getStatus() == RequestStatus.CONFIRMED;
        request.setStatus(RequestStatus.CANCELED);
        requestRepository.save(request);
        if (wasConfirmed) {
            eventFeignClient.adjustConfirmedRequestCount(request.getEventId(), -1);
        }

        return requestMapper.toDtoOut(request);
    }

    /**
     * Returns all participation requests for an event owned by the specified user.
     *
     * @param userId identifier of the event initiator
     * @param eventId identifier of the event
     * @return all participation requests submitted for the event
     */
    @Override
    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> getEventRequestsByInitiator(Long userId, Long eventId) {
        requestValidator.validateEventRequestsAccess(userId, eventId);
        return requestRepository.findAllByEventId(eventId).stream()
                .map(requestMapper::toDtoOut)
                .toList();
    }

    /**
     * Applies an organizer's status update to pending participation requests.
     * When confirming requests would exceed the participant limit, confirms requests up to the remaining capacity
     * and rejects the rest.
     * An empty request ID list means that all pending requests for the event are processed.
     *
     * @param userId identifier of the event organizer
     * @param eventId identifier of the event
     * @param update requested status and optional request identifiers
     * @return the confirmed and rejected requests
     */
    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeRequestsStatus(Long userId, Long eventId,
                                                               EventRequestStatusUpdateRequest update) {
        RequestStatusUpdateContext context = requestValidator.validateChangeRequestsStatus(userId, eventId, update);
        return switch (context.status()) {
            case CONFIRMED -> confirmRequestsWithinLimit(eventId, context);
            case REJECTED -> rejectRequests(context.requests());
            default -> throw new IllegalStateException("The validator returned an unsupported request status");
        };
    }

    /**
     * Checks whether a user has a confirmed request for an event.
     *
     * @param userId identifier of the requester
     * @param eventId identifier of the event
     * @return {@code true} if the request is confirmed; otherwise {@code false}
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isConfirmedParticipant(Long userId, Long eventId) {
        return requestRepository.existsByRequesterIdAndEventIdAndStatus(
                userId, eventId, RequestStatus.CONFIRMED
        );
    }

    /**
     * Confirms selected requests up to the remaining participant capacity and rejects overflow requests.
     * If the capacity becomes full, all other pending requests for the event are rejected as well.
     *
     * @param eventId identifier of the event
     * @param context validated requests and current event capacity information
     * @return the confirmed and rejected requests
     */
    private EventRequestStatusUpdateResult confirmRequestsWithinLimit(Long eventId,
                                                                      RequestStatusUpdateContext context) {
        long confirmedCount = context.confirmedRequestCount();
        int participantLimit = context.participantLimit();
        List<ParticipationRequest> selectedRequests = context.requests();
        List<ParticipationRequest> confirmedRequests = new ArrayList<>();
        List<ParticipationRequest> rejectedRequests;

        if (confirmedCount + selectedRequests.size() <= participantLimit) {
            selectedRequests.forEach(request -> request.setStatus(RequestStatus.CONFIRMED));
            confirmedRequests.addAll(selectedRequests);
            rejectedRequests = confirmedCount + selectedRequests.size() == participantLimit
                    ? rejectOtherPendingRequests(eventId, selectedRequests)
                    : Collections.emptyList();
        } else {
            rejectedRequests = new ArrayList<>(rejectOtherPendingRequests(eventId, selectedRequests));
            for (ParticipationRequest request : selectedRequests) {
                if (confirmedCount < participantLimit) {
                    request.setStatus(RequestStatus.CONFIRMED);
                    confirmedRequests.add(request);
                    confirmedCount++;
                } else {
                    request.setStatus(RequestStatus.REJECTED);
                    rejectedRequests.add(request);
                }
            }
        }

        requestRepository.saveAll(selectedRequests);
        if (!rejectedRequests.isEmpty()) {
            requestRepository.saveAll(rejectedRequests);
        }
        if (!confirmedRequests.isEmpty()) {
            eventFeignClient.adjustConfirmedRequestCount(eventId, confirmedRequests.size());
        }

        return toStatusUpdateResult(rejectedRequests, confirmedRequests);
    }

    /**
     * Rejects pending requests for the event that were not explicitly selected in the status update.
     *
     * @param eventId identifier of the event
     * @param selectedRequests requests explicitly selected for the status update
     * @return the other pending requests that were rejected
     */
    private List<ParticipationRequest> rejectOtherPendingRequests(Long eventId,
                                                                  List<ParticipationRequest> selectedRequests) {
        Set<Long> selectedIds = new HashSet<>();
        selectedRequests.forEach(request -> selectedIds.add(request.getId()));

        List<ParticipationRequest> rejectedRequests = selectedIds.isEmpty()
                ? requestRepository.findAllByEventIdAndStatus(eventId, RequestStatus.PENDING)
                : requestRepository.findAllByEventIdAndStatusAndIdNotIn(
                        eventId, RequestStatus.PENDING, selectedIds
                );
        rejectedRequests.forEach(request -> request.setStatus(RequestStatus.REJECTED));
        return rejectedRequests;
    }

    /**
     * Rejects the selected participation requests.
     *
     * @param requests pending requests to reject
     * @return a status update result containing the rejected requests
     */
    private EventRequestStatusUpdateResult rejectRequests(List<ParticipationRequest> requests) {
        requests.forEach(request -> request.setStatus(RequestStatus.REJECTED));
        requestRepository.saveAll(requests);
        return toStatusUpdateResult(requests, Collections.emptyList());
    }

    /**
     * Maps updated request entities into the API response.
     *
     * @param rejectedRequests rejected requests
     * @param confirmedRequests confirmed requests
     * @return the status update result
     */
    private EventRequestStatusUpdateResult toStatusUpdateResult(List<ParticipationRequest> rejectedRequests,
                                                                List<ParticipationRequest> confirmedRequests) {
        List<ParticipationRequestDto> rejectedDtos = rejectedRequests.stream()
                .map(requestMapper::toDtoOut)
                .toList();
        List<ParticipationRequestDto> confirmedDtos = confirmedRequests.stream()
                .map(requestMapper::toDtoOut)
                .toList();
        return EventRequestStatusUpdateResult.builder()
                .rejectedRequests(rejectedDtos)
                .confirmedRequests(confirmedDtos)
                .build();
    }
}

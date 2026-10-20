package ru.practicum.main.event.service.privateapi;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.practicum.main.category.model.Category;
import ru.practicum.main.category.repository.CategoryRepository;
import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.EventRequestStatusUpdateRequest;
import ru.practicum.main.event.dto.EventRequestStatusUpdateResult;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.dto.NewEventDto;
import ru.practicum.main.event.dto.StateActionUser;
import ru.practicum.main.event.dto.UpdateEventUserRequest;
import ru.practicum.main.event.dto.mapper.EventMapper;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.model.EventState;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.event.service.EventStatsCollector;
import ru.practicum.main.exception.EventUpdateException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.request.dto.ParticipationRequestDto;
import ru.practicum.main.request.dto.mapper.RequestMapper;
import ru.practicum.main.request.model.ParticipationRequest;
import ru.practicum.main.request.model.RequestStatus;
import ru.practicum.main.request.repository.RequestRepository;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.time.LocalDateTime.now;
import static ru.practicum.main.util.EwmConstants.DATE_TIME_FORMATTER;

@Service
@RequiredArgsConstructor
@Transactional
public class EventServicePrivateImpl implements EventServicePrivate {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;

    private final EventMapper eventMapper;
    private final RequestMapper requestMapper;
    private final EventStatsCollector eventStatsCollector;

    @Override
    public EventFullDto create(Long userId, NewEventDto newEventDto) {
        User initiator = getUser(userId);

        Long categoryId = newEventDto.getCategoryId();
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException((String.format("Category with id = %d not found", categoryId))));

        Event event = eventMapper.toEntity(newEventDto, initiator, category);

        event = eventRepository.save(event);

        return eventMapper.toFullDto(event, 0L, 0L, 5.0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> findAllByInitiatorId(Long userId, Integer from, Integer size) {
        getUser(userId); // only for user existence checking

        Pageable pageable = PageRequest.of(from / size, size);

        List<Event> events = eventRepository.findByInitiatorId(userId, pageable);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        return eventStatsCollector.getShortDtoListWithStats(events);
    }

    /**
     * Retrieves full details of a specific event created by a specific initiator.
     *
     * @param userId  the unique identifier of the event initiator
     * @param eventId the unique identifier of the requested event
     * @return the populated {@link EventFullDto} with views and confirmed requests
     * @throws NotFoundException if user or event does not exist, or event does not belong to the user
     */
    @Override
    @Transactional(readOnly = true)
    public EventFullDto findByInitiatorAndEventIds(Long userId, Long eventId) {
        getUser(userId); // only for user existence checking

        Event event = getEventByIdAndInitiator(userId, eventId); // Validate event by user and throws exception if no access

        List<EventFullDto> result = eventStatsCollector.getFullDtoListWithStats(List.of(event));

        return result.getFirst();
    }

    /**
     * Updates an existing event by its initiator.
     * <p>
     * Validates the event's eligibility for update, applies non-null fields from the request DTO,
     * saves the updated entity, and enriches the result with current views and confirmed requests.
     *
     * @param request the DTO containing updated event details
     * @param userId  the ID of the user initiating the update
     * @param eventId the ID of the event to be updated
     * @return the updated {@link EventFullDto} containing full event details and updated statistics
     * @throws NotFoundException    if the user, event, or specified category is not found
     * @throws EventUpdateException if the event is already published or scheduled to start within 2 hours
     */
    @Override
    public EventFullDto updateEventByInitiator(UpdateEventUserRequest request, Long userId, Long eventId) {
        // Retrieves and validates event: throws NotFoundException if user/event doesn't exist,
        // and EventUpdateException if event is PUBLISHED or starts in less than 2 hours
        Event event = getEventIfValidToUpdate(request, userId, eventId);

        // Updates event entity with non-null fields from request DTO, including category and state transition
        updateEventFromNotNullDtoFields(request, event);

        eventRepository.save(event);

        List<EventFullDto> result = eventStatsCollector.getFullDtoListWithStats(List.of(event));

        return result.getFirst();
    }

    /**
     * Retrieves all participation requests submitted for a specific event.
     * <p>
     * Validates that the requesting user exists and is the registered initiator of the event
     * before fetching the associated participation requests.
     * </p>
     *
     * @param userId  the unique identifier of the event initiator
     * @param eventId the unique identifier of the target event
     * @return a {@link List} of {@link ParticipationRequestDto} representing the participation requests for the event
     * @throws NotFoundException if the user or event does not exist, or if the event does not belong to the user
     */
    @Transactional(readOnly = true)
    @Override
    public List<ParticipationRequestDto> getEventRequestsByInitiator(Long userId, Long eventId) {
        getUser(userId); // only for user existence checking
        getEventByIdAndInitiator(userId, eventId); // Validate event by user and throws exception if no access

        List<ParticipationRequest> requests = requestRepository.findAllByEventId(eventId);

        return requests.stream().map(requestMapper::toDtoOut).toList();
    }

    @Override
    public EventRequestStatusUpdateResult changeRequestsStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest incomingRequestDto) {
        Event event = getEventByIdAndInitiator(userId, eventId); // Validate event by user and throws exception if no access
        Long confReq = getConfirmedRequestsAmountOrThrow(event); // throws exception if no need to confirm or limit has reached
        List<ParticipationRequest> participationRequests = getRequestsIfExistOrThrow(incomingRequestDto, eventId); //returns list of requests or throw exception if not found by id

        checkStatusIsPendingOrThrow(participationRequests);

        RequestStatus newStatus = incomingRequestDto.getStatus();

        EventRequestStatusUpdateResult result;

        switch (newStatus) {
            case CONFIRMED ->
                    result = processConfirmationWithLimit(incomingRequestDto, event, participationRequests, confReq);
            case REJECTED -> result = processRejecting(participationRequests);
            default -> throw new EventUpdateException(
                    String.format("Unsupported status action: %s", newStatus));
        }

        return result;
    }

    /**
     * Retrieves a {@link User} entity by its ID from the repository.
     *
     * @param userId the unique identifier of the user to fetch
     * @return the found {@link User} entity
     * @throws NotFoundException if no user with the specified ID exists
     */
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("User with id = %d not found", userId)));
    }


    /**
     * Retrieves an {@link Event} entity by its ID and ensures it belongs to the specified initiator.
     *
     * @param userId  the unique identifier of the event initiator
     * @param eventId the unique identifier of the event to fetch
     * @return the found {@link Event} entity
     * @throws NotFoundException if the event does not exist or does not belong to the specified user
     */
    private Event getEventByIdAndInitiator(Long userId, Long eventId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Event with id = %d not found for user with id = %d", eventId, userId)));
    }

    /**
     * Retrieves an event by its ID and initiator ID after validating that it exists,
     * belongs to the user, and meets all prerequisites for updating.
     * <p>
     * Validates that the event is not in a {@code PUBLISHED} state, and that the event date
     * is at least 2 hours in the future (unless a valid new date is provided in the request).
     *
     * @param request the DTO containing updated event details
     * @param userId  the ID of the user requesting the update
     * @param eventId the ID of the event to be updated
     * @return the validated {@link Event} entity ready for modification
     * @throws NotFoundException    if either the user or the event is not found
     * @throws EventUpdateException if the event is published or scheduled within 2 hours without a valid date postponement
     */
    private Event getEventIfValidToUpdate(UpdateEventUserRequest request, Long userId, Long eventId) {
        getUser(userId); // only for user existence checking
        Event event = getEventByIdAndInitiator(userId, eventId); // Validate event by user and throws exception if no access

        if (event.getState() == EventState.PUBLISHED) {
            String message = String.format("Event with id = %d cannot be updated because its status is PUBLISHED", eventId);
            throw new EventUpdateException(message);
        }

        LocalDateTime deadline = now().plusHours(2);
        if (event.getEventDate().isBefore(deadline) && ((request.getEventDate() == null) || request.getEventDate().isBefore(deadline))) {
            String message = String.format("Event with id = %d cannot be updated because event date %s is less than 2 hours from now", eventId, event.getEventDate().format(DATE_TIME_FORMATTER));
            throw new EventUpdateException(message);
        }
        return event;
    }

    /**
     * Updates non-null fields of the existing {@link Event} entity based on the provided {@link UpdateEventUserRequest}.
     * <p>
     * Performs partial mapping of basic scalar fields via {@link EventMapper}, fetches and assigns
     * a new {@link Category} if {@code categoryId} is present, and updates the {@link EventState}
     * according to the requested {@link StateActionUser}.
     *
     * @param request the DTO containing updated event details
     * @param event   the target {@link Event} entity to be updated
     * @throws NotFoundException if the category specified by {@code categoryId} does not exist
     */
    private void updateEventFromNotNullDtoFields(UpdateEventUserRequest request, Event event) {
        eventMapper.updateEventFromUserDto(request, event);

        if (request.getCategoryId() != null) {
            Category newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException(String.format("Category with id = %d was not found", request.getCategoryId())));
            event.setCategory(newCategory);
        }
        StateActionUser stateActionUser = request.getStateAction();

        switch (stateActionUser) {
            case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
            case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
            case null, default -> { /*Do nothing*/ }
        }
    }

    /**
     * Retrieves all participation requests specified in the status update request.
     * <p>
     * Validates that every participation request ID provided in the request exists in the database.
     * If one or more requested IDs are missing, a {@link NotFoundException} is thrown to ensure
     * atomic processing.
     * </p>
     *
     * @param incomingRequestDto the request DTO containing the list of participation request IDs to update
     * @return a {@link List} of found {@link ParticipationRequest} entities
     * @throws NotFoundException if any of the specified request IDs are not found in the database
     */
    private List<ParticipationRequest> getRequestsIfExistOrThrow(EventRequestStatusUpdateRequest incomingRequestDto, Long eventId) {
        List<Long> requestIds = incomingRequestDto.getRequestsIds();

        if (CollectionUtils.isEmpty(requestIds)) {
            return requestRepository.findAllByEventId(eventId);
        }

        Set<Long> uniqueIds = new HashSet<>(requestIds);
        List<ParticipationRequest> participationRequests = requestRepository.findAllById(uniqueIds);

        if (participationRequests.size() != uniqueIds.size()) {
            String message = String.format("Some requests were not found among IDs: %s", requestIds);
            throw new NotFoundException(message);
        }

        return participationRequests;
    }

    /**
     * Rejects all remaining pending participation requests for a given event, excluding those
     * already specified in the incoming status update request.
     *
     * @param incomingRequestDto the DTO containing the list of request IDs currently being processed
     * @param eventId            the unique identifier of the event
     * @return a {@link List} of newly rejected {@link ParticipationRequest} entities
     */
    private List<ParticipationRequest> rejectAllOtherPendingRequests(EventRequestStatusUpdateRequest incomingRequestDto, Long eventId) {
        List<ParticipationRequest> allRequests = requestRepository.findAllByEventId(eventId);

        List<Long> requestsIds = incomingRequestDto.getRequestsIds();

        Set<Long> uniqueIds = requestsIds == null ? Collections.emptySet() : new HashSet<>(incomingRequestDto.getRequestsIds());

        List<ParticipationRequest> rejected = allRequests.stream()
                .filter(request -> request.getStatus() == RequestStatus.PENDING)
                .filter(request -> !uniqueIds.contains(request.getId()))
                .toList();

        rejected.forEach(request -> request.setStatus(RequestStatus.REJECTED));

        return rejected;
    }

    /**
     * Validates whether the event requires request moderation and if the participant limit has been reached.
     *
     * @param event the {@link Event} entity to check
     * @return the current number of confirmed participation requests for the event
     * @throws EventUpdateException if moderation is disabled, participant limit is 0, or the limit is reached
     */
    private Long getConfirmedRequestsAmountOrThrow(Event event) {
        Integer limit = event.getParticipantLimit();
        Long eventId = event.getId();

        if (limit == 0 || !event.getRequestModeration()) {
            throw new EventUpdateException("Confirmation is not required for events with 0 limit or disabled moderation");
        }

        List<Event> events = List.of(event);
        Map<Long, Long> confirmedRequests = eventStatsCollector.getConReqByEventMap(events);
        Long confReq = confirmedRequests.getOrDefault(eventId, 0L);

        if (confReq == limit.longValue()) {
            String message = String.format("The participant limit has been reached for event id = %d", eventId);
            throw new EventUpdateException(message);
        }

        return confReq;
    }


    /**
     * Ensures that all specified participation requests are in the {@link RequestStatus#PENDING} status.
     *
     * @param participationRequests the list of {@link ParticipationRequest} entities to validate
     * @throws EventUpdateException if any request status is not {@link RequestStatus#PENDING}
     */
    private void checkStatusIsPendingOrThrow(List<ParticipationRequest> participationRequests) {
        participationRequests.forEach(request -> {
            if (!request.getStatus().equals(RequestStatus.PENDING)) {
                String message = String.format("Confirmation is required only for events (id = %d) with PENDING status", request.getId());
                throw new EventUpdateException(message);
            }
        });
    }


    /**
     * Processes the confirmation of participation requests while respecting the event's participant limit.
     * <p>
     * Requests are confirmed up to the maximum limit. Any excess requests within the current batch
     * or remaining pending requests in the database are automatically rejected.
     * </p>
     *
     * @param incomingRequestDto    the DTO containing the list of request IDs to process
     * @param event                 the target {@link Event} entity
     * @param participationRequests the list of fetched {@link ParticipationRequest} entities to update
     * @param confReq               the current number of confirmed requests prior to this update
     * @return an {@link EventRequestStatusUpdateResult} containing DTO lists of confirmed and rejected requests
     */
    private EventRequestStatusUpdateResult processConfirmationWithLimit(EventRequestStatusUpdateRequest incomingRequestDto, Event event, List<ParticipationRequest> participationRequests, Long confReq) {
        Long eventId = event.getId();
        final int limit = event.getParticipantLimit();
        final int updateAmount = participationRequests.size();
        final long confReqAfterUpdate = confReq + updateAmount;

        List<ParticipationRequest> rejectedRequests;
        List<ParticipationRequest> newConfirmedRequests = new ArrayList<>();

        if (confReqAfterUpdate <= limit) {
            participationRequests.forEach(request -> request.setStatus(RequestStatus.CONFIRMED));
            newConfirmedRequests.addAll(participationRequests);

            if (confReqAfterUpdate == limit) {
                rejectedRequests = rejectAllOtherPendingRequests(incomingRequestDto, eventId);
            } else {
                rejectedRequests = Collections.emptyList();
            }

        } else {
            rejectedRequests = new ArrayList<>(rejectAllOtherPendingRequests(incomingRequestDto, eventId));

            for (ParticipationRequest pR : participationRequests) {
                if (confReq < limit) {
                    pR.setStatus(RequestStatus.CONFIRMED);
                    newConfirmedRequests.add(pR);
                    confReq++;
                } else {
                    pR.setStatus(RequestStatus.REJECTED);
                    rejectedRequests.add(pR);
                }
            }
        }

        requestRepository.saveAll(participationRequests);
        if (!rejectedRequests.isEmpty()) {
            requestRepository.saveAll(rejectedRequests);
        }

        return getEventRequestStatusUpdateResult(rejectedRequests, newConfirmedRequests);
    }

    /**
     * Processes explicit rejection of the specified participation requests.
     *
     * @param participationRequests the list of target requests to reject
     * @return an {@link EventRequestStatusUpdateResult} containing the rejected request DTOs
     */
    private EventRequestStatusUpdateResult processRejecting(List<ParticipationRequest> participationRequests) {
        participationRequests.forEach(request -> request.setStatus(RequestStatus.REJECTED));

        requestRepository.saveAll(participationRequests);

        return getEventRequestStatusUpdateResult(participationRequests, Collections.emptyList());
    }

    /**
     * Maps lists of rejected and confirmed request entities into an {@link EventRequestStatusUpdateResult} DTO.
     *
     * @param rejectedRequests     the list of rejected {@link ParticipationRequest} entities
     * @param newConfirmedRequests the list of confirmed {@link ParticipationRequest} entities
     * @return the constructed {@link EventRequestStatusUpdateResult} DTO
     */
    private EventRequestStatusUpdateResult getEventRequestStatusUpdateResult(List<ParticipationRequest> rejectedRequests, List<ParticipationRequest> newConfirmedRequests) {
        List<ParticipationRequestDto> rejectedDto = rejectedRequests.stream()
                .map(requestMapper::toDtoOut)
                .toList();

        List<ParticipationRequestDto> newConfirmedDto = newConfirmedRequests.stream()
                .map(requestMapper::toDtoOut)
                .toList();

        return EventRequestStatusUpdateResult.builder()
                .rejectedRequests(rejectedDto)
                .confirmedRequests(newConfirmedDto)
                .build();
    }
}

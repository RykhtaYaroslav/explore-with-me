package ru.practicum.core.event.service.privateapi;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.core.category.model.Category;
import ru.practicum.core.category.repository.CategoryRepository;
import ru.practicum.core.common.exception.EventUpdateException;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.event.dto.mapper.EventMapper;
import ru.practicum.core.event.dto.request.NewEventDto;
import ru.practicum.core.event.dto.request.StateActionUser;
import ru.practicum.core.event.dto.request.UpdateEventUserRequest;
import ru.practicum.core.event.dto.response.EventFullDto;
import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.core.event.model.Event;
import ru.practicum.core.event.model.EventState;
import ru.practicum.core.event.repository.EventRepository;
import ru.practicum.core.event.service.EventStatsCollector;
import ru.practicum.core.event.service.interservice.RecommendationInterservice;
import ru.practicum.core.event.service.interservice.UserInterservice;
import ru.practicum.core.user.dto.UserShortDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.time.LocalDateTime.now;
import static ru.practicum.core.common.util.EwmConstants.DATE_TIME_FORMATTER;

@Service
@RequiredArgsConstructor
@Transactional
public class EventServicePrivateImpl implements EventServicePrivate {
    private final EventRepository eventRepository;
    private final UserInterservice userInterservice;
    private final CategoryRepository categoryRepository;

    private final EventMapper eventMapper;
    private final EventStatsCollector eventStatsCollector;
    private final RecommendationInterservice recommendations;

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> getRecommendations(long userId, int size) {
        userInterservice.checkExistsByIdOrThrow(userId);
        List<Long> recommendedIds = recommendations.getRecommendedEventIds(userId, size);
        if (recommendedIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Event> publishedEvents = eventRepository.findAllWithCategoryByIdIn(recommendedIds).stream()
                .filter(event -> event.getState() == EventState.PUBLISHED)
                .collect(Collectors.toMap(Event::getId, event -> event));
        List<Event> orderedEvents = recommendedIds.stream()
                .map(publishedEvents::get)
                .filter(Objects::nonNull)
                .toList();
        return eventStatsCollector.getShortDtoListWithStats(orderedEvents);
    }

    @Override
    public EventFullDto create(Long userId, NewEventDto newEventDto) {
        UserShortDto initiator = userInterservice.getShortById(userId);

        Long categoryId = newEventDto.getCategoryId();
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException((String.format("Category with id = %d not found", categoryId))));

        Event event = eventMapper.toEntity(newEventDto, userId, category);

        event = eventRepository.save(event);

        return eventMapper.toFullDto(event, initiator, 0L, 0L, 5.0, 0.0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventShortDto> findAllByInitiatorId(Long userId, Integer from, Integer size) {
        userInterservice.checkExistsByIdOrThrow(userId);

        Pageable pageable = PageRequest.of(from / size, size);

        List<Event> events = eventRepository.findByInitiatorId(userId, pageable);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        return eventStatsCollector.getShortDtoListWithStats(events);
    }

    /**
     * Retrieves full details of a specific eventId created by a specific initiator.
     *
     * @param userId  the unique identifier of the eventId initiator
     * @param eventId the unique identifier of the requested eventId
     * @return the populated {@link EventFullDto} with views and confirmed requests
     * @throws NotFoundException if user or eventId does not exist, or eventId does not belong to the user
     */
    @Override
    @Transactional(readOnly = true)
    public EventFullDto findByInitiatorAndEventIds(Long userId, Long eventId) {
        userInterservice.checkExistsByIdOrThrow(userId);

        Event event = getEventByIdAndInitiator(userId, eventId); // Validate eventId by user and throws exception if no access

        List<EventFullDto> result = eventStatsCollector.getFullDtoListWithStats(List.of(event));

        return result.getFirst();
    }

    /**
     * Updates an existing eventId by its initiator.
     * <p>
     * Validates the eventId's eligibility for update, applies non-null fields from the request DTO,
     * saves the updated entity, and enriches the result with current views and confirmed requests.
     *
     * @param request the DTO containing updated eventId details
     * @param userId  the ID of the user initiating the update
     * @param eventId the ID of the eventId to be updated
     * @return the updated {@link EventFullDto} containing full eventId details and updated statistics
     * @throws NotFoundException    if the user, eventId, or specified category is not found
     * @throws EventUpdateException if the eventId is already published or scheduled to start within 2 hours
     */
    @Override
    public EventFullDto updateEventByInitiator(UpdateEventUserRequest request, Long userId, Long eventId) {
        // Retrieves and validates eventId: throws NotFoundException if user/eventId doesn't exist,
        // and EventUpdateException if eventId is PUBLISHED or starts in less than 2 hours
        Event event = getEventIfValidToUpdate(request, userId, eventId);

        // Updates eventId entity with non-null fields from request DTO, including category and state transition
        updateEventFromNotNullDtoFields(request, event);

        eventRepository.save(event);

        List<EventFullDto> result = eventStatsCollector.getFullDtoListWithStats(List.of(event));

        return result.getFirst();
    }


    /**
     * Retrieves an {@link Event} entity by its ID and ensures it belongs to the specified initiator.
     *
     * @param userId  the unique identifier of the eventId initiator
     * @param eventId the unique identifier of the eventId to fetch
     * @return the found {@link Event} entity
     * @throws NotFoundException if the eventId does not exist or does not belong to the specified user
     */
    private Event getEventByIdAndInitiator(Long userId, Long eventId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Event with id = %d not found for user with id = %d", eventId, userId)));
    }

    /**
     * Retrieves an eventId by its ID and initiator ID after validating that it exists,
     * belongs to the user, and meets all prerequisites for updating.
     * <p>
     * Validates that the eventId is not in a {@code PUBLISHED} state, and that the eventId date
     * is at least 2 hours in the future (unless a valid new date is provided in the request).
     *
     * @param request the DTO containing updated eventId details
     * @param userId  the ID of the user requesting the update
     * @param eventId the ID of the eventId to be updated
     * @return the validated {@link Event} entity ready for modification
     * @throws NotFoundException    if either the user or the eventId is not found
     * @throws EventUpdateException if the eventId is published or scheduled within 2 hours without a valid date postponement
     */
    private Event getEventIfValidToUpdate(UpdateEventUserRequest request, Long userId, Long eventId) {
        userInterservice.checkExistsByIdOrThrow(userId);

        Event event = getEventByIdAndInitiator(userId, eventId); // Validate eventId by user and throws exception if no access

        if (event.getState() == EventState.PUBLISHED) {
            String message = String.format("Event with id = %d cannot be updated because its status is PUBLISHED", eventId);
            throw new EventUpdateException(message);
        }

        LocalDateTime deadline = now().plusHours(2);
        if (event.getEventDate().isBefore(deadline) && ((request.getEventDate() == null) || request.getEventDate().isBefore(deadline))) {
            String message = String.format("Event with id = %d cannot be updated because eventId date %s is less than 2 hours from now", eventId, event.getEventDate().format(DATE_TIME_FORMATTER));
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
     * @param request the DTO containing updated eventId details
     * @param event   the targetId {@link Event} entity to be updated
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
}

package ru.practicum.core.event.service.adminapi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.core.category.model.Category;
import ru.practicum.core.category.repository.CategoryRepository;
import ru.practicum.core.common.exception.EventUpdateException;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.event.dto.EventQueryParams;
import ru.practicum.core.event.dto.mapper.EventMapper;
import ru.practicum.core.event.dto.request.StateActionAdmin;
import ru.practicum.core.event.dto.request.UpdateEventAdminRequest;
import ru.practicum.core.event.dto.response.EventFullDto;
import ru.practicum.core.event.model.Event;
import ru.practicum.core.event.model.EventState;
import ru.practicum.core.event.repository.EventRepository;
import ru.practicum.core.event.service.EventStatsCollector;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EventServiceAdminImpl implements EventServiceAdmin {
    private final EventRepository eventRepository;
    private final EventStatsCollector eventStatsCollector;

    private final EventMapper eventMapper;

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EventFullDto> getAllFullInfoWithParams(EventQueryParams params) {
        List<Event> events = getEventsWithParamsFromRepository(params);

        List<EventFullDto> result = eventStatsCollector.getFullDtoListWithStats(events);
        log.debug("Admin event search confirmed request counts (eventId:count): {}",
                result.stream()
                        .map(event -> event.getId() + ":" + event.getConfirmedRequests())
                        .toList());
        return result;
    }

    @Override
    public EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id = %d not found", eventId)));

        updateEventFromNotNullDtoFields(request, event);
        event = eventRepository.save(event);

        List<Event> events = List.of(event);
        List<EventFullDto> eventFullDtos = eventStatsCollector.getFullDtoListWithStats(events);

        return eventFullDtos.getFirst();
    }

    private List<Event> getEventsWithParamsFromRepository(EventQueryParams params) {

        List<String> states = (params.states() != null)
                ? params.states().stream().map(Enum::name).toList()
                : null;

        String sort = (params.sort() != null) ? params.sort().name() : null;

        Pageable pageable = PageRequest.of(params.from() / params.size(), params.size());

        return eventRepository.findAllWithParams(
                params.text(),
                params.categories(),
                params.paid(),
                params.rangeStart(),
                params.rangeEnd(),
                params.onlyAvailable(),
                sort,
                params.users(),
                states,
                pageable);
    }

    private void updateEventFromNotNullDtoFields(UpdateEventAdminRequest request, Event event) {
        eventMapper.updateEventFromAdminDto(request, event);

        if (request.getCategoryId() != null) {
            Category newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new NotFoundException(String.format("Category with id = %d was not found", request.getCategoryId())));
            event.setCategory(newCategory);
        }
        StateActionAdmin stateActionAdmin = request.getStateAction();

        switch (stateActionAdmin) {
            case PUBLISH_EVENT -> {
                if (event.getState() != EventState.PENDING) {
                    String m = String.format("Cannot publish the eventId because it's not in the PENDING state: %s", event.getState());
                    throw new EventUpdateException(m);
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            }
            case REJECT_EVENT -> {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new EventUpdateException("Cannot reject the eventId because it's already PUBLISHED");
                }
                event.setState(EventState.CANCELED);
            }
            case null, default -> { /*Do nothing*/ }
        }
    }
}

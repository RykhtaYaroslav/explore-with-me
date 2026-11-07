package ru.practicum.core.event.service.publicapi;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.event.dto.EventQueryParams;
import ru.practicum.core.event.dto.request.EventSort;
import ru.practicum.core.event.dto.response.EventFullDto;
import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.core.event.model.Event;
import ru.practicum.core.event.model.EventState;
import ru.practicum.core.event.repository.EventRepository;
import ru.practicum.core.event.service.EventStatsCollector;
import ru.practicum.core.event.service.interservice.RecommendationInterservice;

import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class EventServicePublicImpl implements EventServicePublic {
    private final EventRepository eventRepository;
    private final RecommendationInterservice recommendations;
    private final EventStatsCollector eventStatsCollector;


    @Override
    public List<EventShortDto> getAllWithParams(EventQueryParams params) {

        List<Event> events = getEventsWithParamsFromRepository(params);

        if (events.isEmpty()) {
            return List.of();
        }

        List<EventShortDto> shortDtos = eventStatsCollector.getShortDtoListWithStats(events);

        EventSort sortType = params.sort();

        switch (sortType) {
            case EVENT_DATE -> { /*Do nothing*/ }
            case VIEWS -> shortDtos = shortDtos.stream()
                    .sorted(Comparator.comparingLong(EventShortDto::getViews).reversed())
                    .toList();
            case RATING -> shortDtos = shortDtos.stream()
                    .sorted(Comparator.comparing(
                            EventShortDto::getRating,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ).reversed())
                    .toList();
            case null, default -> { /*Do nothing*/ }
        }

        return shortDtos;
    }

    @Override
    public EventFullDto getEventFullInformation(Long id, Long userId, String ip) {
        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id = %d was not found", id)));

        EventFullDto result = eventStatsCollector.getFullDtoListWithStats(List.of(event)).getFirst();
        eventStatsCollector.recordView(id, ip);
        if (userId != null) {
            recommendations.recordView(userId, id);
        }
        return result;
    }

    /**
     * Retrieves a list of {@link Event} entities from the repository based on the provided public filtering parameters.
     * <p>
     * Preprocesses input parameters prior to repository query execution:
     * <ul>
     *   <li>Formats search {@code text} with wildcard delimiters ({@code %text%}) for SQL {@code LIKE} filtering, or maps to {@code null} if blank.</li>
     *   <li>Converts empty {@code categories} collections to {@code null} to avoid invalid SQL IN clause execution.</li>
     *   <li>Extracts string representations of the sort option if specified.</li>
     * </ul>
     * </p>
     *
     * @param params the {@link EventQueryParams} record containing request filters, pagination options, and sorting strategy
     * @return a {@link List} of filtered {@link Event} entities matching the specified criteria
     */
    private List<Event> getEventsWithParamsFromRepository(EventQueryParams params) {
        String text = (params.text() == null || params.text().isBlank()) ? null : params.text().toLowerCase();

        List<Long> categories = CollectionUtils.isEmpty(params.categories()) ? null : params.categories();

        String sort = (params.sort() != null) ? params.sort().name() : null;

        List<String> states = params.states().stream().map(Enum::name).toList();

        return eventRepository.findAllWithParams(
                text,
                categories,
                params.paid(),
                params.rangeStart(),
                params.rangeEnd(),
                params.onlyAvailable(),
                sort,
                params.users(),
                states,
                PageRequest.of(params.from() / params.size(), params.size()));
    }
}

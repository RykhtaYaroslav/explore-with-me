package ru.practicum.main.event.service.publicapi;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.EventQueryParams;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.dto.EventSort;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.model.EventState;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.event.service.EventStatsCollector;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.EndpointHitDto;

import java.util.Comparator;
import java.util.List;

import static java.time.LocalDateTime.now;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class EventServicePublicImpl implements EventServicePublic {
    private final EventRepository eventRepository;
    private final StatsClient statsRepository;

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
    public EventFullDto getEventFullInformation(Long id) {
        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id = %d was not found", id)));

        List<Event> events = List.of(event);

        return eventStatsCollector.getFullDtoListWithStats(events).getFirst();
    }

    @Override
    public void hitStat(String uri, String ip) {
        statsRepository.hit(EndpointHitDto.builder()
                .app("main-service")
                .ip(ip)
                .uri(uri)
                .timestamp(now())
                .build());
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

        Pageable pageable = PageRequest.of(params.from() / params.size(), params.size());

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
                pageable);
    }
}

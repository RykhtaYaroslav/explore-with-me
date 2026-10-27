package ru.practicum.core.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.core.common.dto.participation.ConfirmedRequestsCount;
import ru.practicum.core.common.enums.RequestStatus;
import ru.practicum.core.event.dto.EventRatingCount;
import ru.practicum.core.event.dto.mapper.EventMapper;
import ru.practicum.core.event.dto.response.EventFullDto;
import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.core.event.model.Event;
import ru.practicum.core.event.service.interservice.ParticipationRequestInterservice;
import ru.practicum.core.event.service.interservice.ReviewInterservice;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.time.LocalDateTime.now;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventStatsCollectorImpl implements EventStatsCollector {
    private final StatsClient statsRepository;
    private final ParticipationRequestInterservice requestExternalService;
    private final ReviewInterservice reviewInterservice;

    private final EventMapper eventMapper;

    @Override
    public List<EventShortDto> getShortDtoListWithStats(List<Event> events) {
        Map<Long, Long> viewsByEventMap = getViewsByEventMap(events);
        Map<Long, Long> confirmedRequestsByEventMap = getConReqByEventMap(events);
        Map<Long, Double> ratingByEventMap = getRatingMap(events);

        return events.stream()
                .map(event -> {
                    Long id = event.getId();
                    return eventMapper.toShortDto(event,
                            viewsByEventMap.getOrDefault(id, 0L),
                            confirmedRequestsByEventMap.getOrDefault(id, 0L),
                            ratingByEventMap.getOrDefault(id, 5.0));
                })
                .toList();
    }

    @Override
    public List<EventFullDto> getFullDtoListWithStats(List<Event> events) {
        Map<Long, Long> views = getViewsByEventMap(events);
        Map<Long, Long> confirmedRequests = getConReqByEventMap(events);

        Map<Long, Double> ratings = getRatingMap(events);

        return events.stream()
                .map(event -> eventMapper.toFullDto(
                        event,
                        views.getOrDefault(event.getId(), 0L),
                        confirmedRequests.getOrDefault(event.getId(), 0L),
                        ratings.getOrDefault(event.getId(), 5.0)
                ))
                .toList();
    }

    /**
     * Maps each event ID to its total number of views collected from the statistics service.
     *
     * @param events the list of {@link Event} entities to fetch statistics for
     * @return a {@link Map} where the key is the event ID and the value is the total view count
     */
    @Override
    public Map<Long, Long> getViewsByEventMap(List<Event> events) {
        // This method fills Map with eventId - views amount

        // Pull statistics about events from stat module
        List<ViewStatsDto> viewStatsDto = getViewStatsDtoList(events);

        return viewStatsDto.stream()
                .collect(Collectors.toMap(
                        dto -> extractIdFromUri(dto.getUri()),
                        dto -> dto.getHits() != null ? dto.getHits() : 0L,
                        Long::max
                ));

    }

    /**
     * Requests view statistics from the remote stats service for a collection of events.
     *
     * @param events the list of {@link Event} entities for which to query view statistics
     * @return a list of {@link ViewStatsDto} containing hit counts for the provided event URIs
     */
    private List<ViewStatsDto> getViewStatsDtoList(List<Event> events) {
        // This method pulls statistics about events from stat module

        LocalDateTime now = now();

        List<String> uris = events.stream()
                .map(event -> String.format("/events/%d", event.getId()))
                .toList();

        LocalDateTime minDate = events.stream()
                .map(Event::getCreatedOn)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(now.minusYears(10));

        return statsRepository.getStats(minDate, now, uris, true);
    }

    /**
     * Extracts the event entity ID from an endpoint URI string (e.g., "/events/42" -> 42L).
     *
     * @param uri the URI path containing the event identifier at the end
     * @return the extracted event ID as a {@link Long}
     */
    private Long extractIdFromUri(String uri) {
        return Long.parseLong(uri.substring((uri.lastIndexOf('/') + 1)));
    }

    /**
     * Maps each event ID to the total number of its confirmed participation requests.
     *
     * @param events the list of {@link Event} entities to aggregate requests for
     * @return a {@link Map} where the key is the event ID and the value is the confirmed request count
     */
    @Override
    public Map<Long, Long> getConReqByEventMap(List<Event> events) {
        List<Long> eventIds = events.stream().map(Event::getId).toList();

        List<ConfirmedRequestsCount> confirmedRequestsCounts = requestExternalService.countRequestsCountByEventIds(eventIds, RequestStatus.CONFIRMED);

        return confirmedRequestsCounts.stream()
                .collect(Collectors.toMap(
                        ConfirmedRequestsCount::eventId,
                        ConfirmedRequestsCount::count
                ));
    }

    /**
     * Retrieves a mapping of event IDs to their average ratings for the specified list of events.
     *
     * @param events the list of events for which to calculate and fetch ratings
     * @return a map where the key is the event ID and the value is its average rating
     */
    private Map<Long, Double> getRatingMap(List<Event> events) {
        List<Long> eventIds = events.stream().map(Event::getId).toList();

        List<EventRatingCount> eventRatingCountList = reviewInterservice.countRatingByIds(eventIds);

        return eventRatingCountList.stream()
                .collect(Collectors.toMap(
                        EventRatingCount::eventId,
                        EventRatingCount::rating
                ));
    }
}

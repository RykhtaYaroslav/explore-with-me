package ru.practicum.core.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.core.event.dto.EventRatingCount;
import ru.practicum.core.event.dto.mapper.EventMapper;
import ru.practicum.core.event.dto.response.EventFullDto;
import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.core.event.model.Event;
import ru.practicum.core.event.service.interservice.ReviewInterservice;
import ru.practicum.core.event.service.interservice.UserInterservice;
import ru.practicum.core.user.dto.UserShortDto;
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
    private final ReviewInterservice reviewInterservice;
    private final UserInterservice userInterservice;

    private final EventMapper eventMapper;

    @Override
    public List<EventShortDto> getShortDtoListWithStats(List<Event> events) {
        Map<Long, Long> viewsByEventMap = getViewsByEventMap(events);
        Map<Long, Long> confirmedRequestsByEventMap = getConReqByEventMap(events);
        Map<Long, Double> ratingByEventMap = getRatingMap(events);
        Map<Long, UserShortDto> initiatorsById = getInitiatorsById(events);

        return events.stream()
                .map(event -> {
                    Long id = event.getId();
                    return eventMapper.toShortDto(event,
                            initiatorsById.get(event.getInitiatorId()),
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
        Map<Long, UserShortDto> initiatorsById = getInitiatorsById(events);

        return events.stream()
                .map(event -> eventMapper.toFullDto(
                        event,
                        initiatorsById.get(event.getInitiatorId()),
                        views.getOrDefault(event.getId(), 0L),
                        confirmedRequests.getOrDefault(event.getId(), 0L),
                        ratings.getOrDefault(event.getId(), 5.0)
                ))
                .toList();
    }

    /**
     * Maps each eventId ID to its total number of views collected from the statistics service.
     *
     * @param events the list of {@link Event} entities to fetch statistics for
     * @return a {@link Map} where the key is the eventId ID and the value is the total view count
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
     * @return a list of {@link ViewStatsDto} containing hit counts for the provided eventId URIs
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
     * Extracts the eventId entity ID from an endpoint URI string (e.g., "/events/42" -> 42L).
     *
     * @param uri the URI path containing the eventId identifier at the end
     * @return the extracted eventId ID as a {@link Long}
     */
    private Long extractIdFromUri(String uri) {
        return Long.parseLong(uri.substring((uri.lastIndexOf('/') + 1)));
    }

    /**
     * Maps each eventId ID to the total number of its confirmed participation requests.
     *
     * @param events the list of {@link Event} entities to aggregate requests for
     * @return a {@link Map} where the key is the eventId ID and the value is the confirmed request count
     */
    @Override
    public Map<Long, Long> getConReqByEventMap(List<Event> events) {
        return events.stream()
                .collect(Collectors.toMap(Event::getId, Event::getConfirmedRequests));
    }

    /**
     * Retrieves a mapping of eventId IDs to their average ratings for the specified list of events.
     *
     * @param events the list of events for which to calculate and fetch ratings
     * @return a map where the key is the eventId ID and the value is its average rating
     */
    private Map<Long, Double> getRatingMap(List<Event> events) {
        List<Long> eventIds = events.stream().map(Event::getId).toList();

        List<EventRatingCount> eventRatingCountList = reviewInterservice.getRatingByIds(eventIds);

        return eventRatingCountList.stream()
                .collect(Collectors.toMap(
                        EventRatingCount::eventId,
                        EventRatingCount::rating
                ));
    }

    private Map<Long, UserShortDto> getInitiatorsById(List<Event> events) {
        List<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .toList();
        if (initiatorIds.isEmpty()) {
            return Map.of();
        }
        return userInterservice.getShortByIds(initiatorIds).stream()
                .collect(Collectors.toMap(UserShortDto::getId, user -> user));
    }
}

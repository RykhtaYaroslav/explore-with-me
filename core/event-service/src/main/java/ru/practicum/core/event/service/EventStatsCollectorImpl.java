package ru.practicum.core.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.core.event.dto.EventRatingCount;
import ru.practicum.core.event.dto.mapper.EventMapper;
import ru.practicum.core.event.dto.response.EventFullDto;
import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.core.event.model.Event;
import ru.practicum.core.event.service.interservice.RecommendationInterservice;
import ru.practicum.core.event.service.interservice.ReviewInterservice;
import ru.practicum.core.event.service.interservice.UserInterservice;
import ru.practicum.core.user.dto.UserShortDto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventStatsCollectorImpl implements EventStatsCollector {
    private final RecommendationInterservice recommendations;
    private final ReviewInterservice reviewInterservice;
    private final UserInterservice userInterservice;

    private final EventMapper eventMapper;

    @Override
    public List<EventShortDto> getShortDtoListWithStats(List<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        }
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> viewsByEventMap = recommendations.getViewCounts(eventIds);
        Map<Long, Long> confirmedRequestsByEventMap = getConReqByEventMap(events);
        Map<Long, Double> ratingByEventMap = getRatingMap(eventIds);
        Map<Long, Double> weights = getInteractionWeights(eventIds);
        Map<Long, UserShortDto> initiatorsById = getInitiatorsById(events);

        return events.stream()
                .map(event -> {
                    Long id = event.getId();
                    return eventMapper.toShortDto(event,
                            initiatorsById.get(event.getInitiatorId()),
                            viewsByEventMap.getOrDefault(id, 0L),
                            confirmedRequestsByEventMap.getOrDefault(id, 0L),
                            ratingByEventMap.getOrDefault(id, 5.0),
                            weights.getOrDefault(id, 0.0));
                })
                .toList();
    }

    @Override
    public List<EventFullDto> getFullDtoListWithStats(List<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        }
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> views = recommendations.getViewCounts(eventIds);
        Map<Long, Long> confirmedRequests = getConReqByEventMap(events);

        Map<Long, Double> ratings = getRatingMap(eventIds);
        Map<Long, Double> weights = getInteractionWeights(eventIds);
        Map<Long, UserShortDto> initiatorsById = getInitiatorsById(events);

        return events.stream()
                .map(event -> eventMapper.toFullDto(
                        event,
                        initiatorsById.get(event.getInitiatorId()),
                        views.getOrDefault(event.getId(), 0L),
                        confirmedRequests.getOrDefault(event.getId(), 0L),
                        ratings.getOrDefault(event.getId(), 5.0),
                        weights.getOrDefault(event.getId(), 0.0)
                ))
                .toList();
    }

    @Override
    public Map<Long, Double> getInteractionWeights(List<Long> eventIds) {
        return recommendations.getInteractionWeights(eventIds);
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
     * @param eventIds the IDs of events for which to fetch ratings
     * @return a map where the key is the eventId ID and the value is its average rating
     */
    private Map<Long, Double> getRatingMap(List<Long> eventIds) {
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

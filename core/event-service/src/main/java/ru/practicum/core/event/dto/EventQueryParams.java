package ru.practicum.core.event.dto;

import lombok.Builder;
import org.springframework.util.CollectionUtils;
import ru.practicum.core.event.dto.request.EventSort;
import ru.practicum.core.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Holds the filters and pagination settings used to search events.
 * Instances are built from either public or administrator search parameters
 * and passed from the controller to the event service. Empty filter lists are
 * normalized to {@code null} so they can be treated as absent filters.
 *
 * @param categories category IDs to include, or {@code null} for all categories
 * @param paid whether to include paid or free events, or {@code null} for either
 * @param rangeStart earliest event date to include, or {@code null} for no lower bound
 * @param rangeEnd latest event date to include, or {@code null} for no upper bound
 * @param from zero-based offset of the first result
 * @param size maximum number of results to return
 * @param text text to match against event titles and annotations, or {@code null} for no text filter
 * @param onlyAvailable whether to include only events with available places, or {@code null} when not specified
 * @param sort public search ordering, or {@code null} when sorting is not requested
 * @param users initiator IDs to include in an administrator search, or {@code null} for all initiators
 * @param states event states to include in an administrator search, or {@code null} for all states
 */
@Builder(toBuilder = true)
public record EventQueryParams(
        // Общие параметры
        List<Long> categories,
        Boolean paid,
        LocalDateTime rangeStart,
        LocalDateTime rangeEnd,
        int from,
        int size,

        // Публичные параметры (для админа будут null)
        String text,
        Boolean onlyAvailable,
        EventSort sort,

        // Админские параметры (для паблика будут заданы специфично или null)
        List<Long> users,
        List<EventState> states
) {
    public EventQueryParams {
        categories = CollectionUtils.isEmpty(categories) ? null : categories;
        users = CollectionUtils.isEmpty(users) ? null : users;
        states = CollectionUtils.isEmpty(states) ? null : states;
    }

    public static EventQueryParams ofPublic(
            String text,
            List<Long> categories,
            Boolean paid,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Boolean onlyAvailable,
            EventSort sort,
            int from,
            int size
    ) {
        return EventQueryParams.builder()
                .text(text)
                .categories(categories)
                .paid(paid)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .onlyAvailable(onlyAvailable)
                .sort(sort)
                .from(from)
                .size(size)
                .states(List.of(EventState.PUBLISHED))
                .users(null)
                .build();
    }

    public static EventQueryParams ofAdmin(
            List<Long> users,
            List<EventState> states,
            List<Long> categories,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            int from,
            int size
    ) {
        return EventQueryParams.builder()
                .users(users)
                .states(states)
                .categories(categories)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .onlyAvailable(Boolean.FALSE)
                .from(from)
                .size(size)
                .text(null)
                .sort(null)
                .paid(null)
                .build();
    }
}

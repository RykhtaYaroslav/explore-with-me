package ru.practicum.main.event.dto;

import lombok.Builder;
import org.springframework.util.CollectionUtils;
import ru.practicum.main.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

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

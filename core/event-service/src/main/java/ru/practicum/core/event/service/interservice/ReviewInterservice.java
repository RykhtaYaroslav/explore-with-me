package ru.practicum.core.event.service.interservice;

import ru.practicum.core.event.dto.EventRatingCount;

import java.util.List;

public interface ReviewInterservice {
    List<EventRatingCount> countRatingByIds(List<Long> ids);
}

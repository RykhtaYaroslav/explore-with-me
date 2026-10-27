package ru.practicum.core.event.service.interservice;

import ru.practicum.core.event.dto.EventRatingCount;

import java.util.List;

public class ReviewInterserviceFeign implements ReviewInterservice {
    @Override
    public List<EventRatingCount> countRatingByIds(List<Long> ids) {
        //TODO: implement this
        return List.of();
    }
}

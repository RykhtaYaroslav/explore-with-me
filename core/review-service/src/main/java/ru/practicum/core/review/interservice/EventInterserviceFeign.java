package ru.practicum.core.review.interservice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.core.review.interservice.feign.EventFeignClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EventInterserviceFeign implements EventInterservice {
    private final EventFeignClient eventFeignClient;

    @Override
    public boolean existsById(Long eventId) {
        return eventFeignClient.existsById(eventId);
    }

    @Override
    public boolean isOrganizer(Long eventId, Long userId) {
        return eventFeignClient.isOrganizer(eventId, userId);
    }

    @Override
    public EventShortDto getReviewInfo(Long eventId) {
        return eventFeignClient.getReviewInfo(eventId);
    }

    @Override
    public List<EventShortDto> getReviewInfoByIds(List<Long> eventIds) {
        return eventFeignClient.getReviewInfoByIds(eventIds);
    }
}

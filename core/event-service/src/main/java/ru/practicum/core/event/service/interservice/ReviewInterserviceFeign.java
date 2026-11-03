package ru.practicum.core.event.service.interservice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.core.event.dto.EventRatingCount;
import ru.practicum.core.event.service.interservice.feign.ReviewFeignClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReviewInterserviceFeign implements ReviewInterservice {
    private final ReviewFeignClient reviewFeignClient;

    @Override
    public List<EventRatingCount> getRatingByIds(List<Long> ids) {
        return reviewFeignClient.getRatingByIds(ids);
    }
}

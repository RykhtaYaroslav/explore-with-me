package ru.practicum.core.event.service.interservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.core.common.constants.ServiceNames;
import ru.practicum.core.event.dto.EventRatingCount;

import java.util.List;

@FeignClient(name = ServiceNames.REVIEW_SERVICE)
public interface ReviewFeignClient {

    @PostMapping("/interservice/reviews/events")
    List<EventRatingCount> getRatingByIds(@RequestBody List<Long> ids);
}

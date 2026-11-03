package ru.practicum.core.review.interservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.core.common.constants.ServiceNames;
import ru.practicum.core.event.dto.response.EventShortDto;

import java.util.List;

@FeignClient(name = ServiceNames.EVENT_SERVICE, contextId = "reviewEventClient")
public interface EventFeignClient {
    @GetMapping("/interservice/events/exist/{eventId}")
    boolean existsById(@PathVariable("eventId") Long eventId);

    @GetMapping("/interservice/events/{eventId}/organizers/{userId}")
    boolean isOrganizer(@PathVariable("eventId") Long eventId, @PathVariable("userId") Long userId);

    @GetMapping("/interservice/events/{eventId}/review-info")
    EventShortDto getReviewInfo(@PathVariable("eventId") Long eventId);

    @GetMapping("/interservice/events/review-info")
    List<EventShortDto> getReviewInfoByIds(@RequestParam("ids") List<Long> eventIds);
}

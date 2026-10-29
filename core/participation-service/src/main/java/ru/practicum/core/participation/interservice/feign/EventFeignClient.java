package ru.practicum.core.participation.interservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.core.common.constants.ServiceNames;
import ru.practicum.core.event.dto.response.EventForParticipationServiceDto;

@FeignClient(name = ServiceNames.EVENT_SERVICE, contextId = "participationEventInfoClient")
public interface EventFeignClient {

    @GetMapping("/interservice/events/{eventId}/participation-info")
    EventForParticipationServiceDto findByIdOrThrow(@PathVariable("eventId") Long eventId);

    @PutMapping("/interservice/events/{eventId}/confirmed-requests")
    void synchronizeConfirmedRequests(@PathVariable("eventId") Long eventId,
                                      @RequestParam("expectedCount") long expectedCount,
                                      @RequestParam("count") long count);
}

package ru.practicum.main.request.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "event-service")
public interface EventFeignClient {

    @PatchMapping("/interservice/events/{eventId}/confirmed-requests")
    void adjustConfirmedRequestCount(@PathVariable("eventId") Long eventId,
                                     @RequestParam("delta") long delta);
}

package ru.practicum.core.participation.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.core.common.constants.ServiceNames;

@FeignClient(name = ServiceNames.EVENT_SERVICE, contextId = "participationEventCounterClient")
public interface EventFeignClient {

    @PatchMapping("/interservice/events/{eventId}/confirmed-requests")
    void adjustConfirmedRequestCount(@PathVariable("eventId") Long eventId,
                                     @RequestParam("delta") long delta);
}

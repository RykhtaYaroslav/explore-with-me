package ru.practicum.core.review.interservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.core.common.constants.ServiceNames;

@FeignClient(name = ServiceNames.MAIN_SERVICE)
public interface RequestFeignClient {
    @GetMapping("/interservice/requests/users/{userId}/events/{eventId}/confirmed")
    boolean isConfirmedParticipant(@PathVariable("userId") Long userId,
                                   @PathVariable("eventId") Long eventId);
    // TODO изменить адрес сервиса на сервис участинков
}

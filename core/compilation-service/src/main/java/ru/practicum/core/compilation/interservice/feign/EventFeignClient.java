package ru.practicum.core.compilation.interservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.core.common.constants.ServiceNames;
import ru.practicum.core.event.dto.response.EventShortDto;

import java.util.List;

@FeignClient(name = ServiceNames.EVENT_SERVICE, contextId = "compilationEventClient")
public interface EventFeignClient {
    @GetMapping("/interservice/events/compilation-info")
    List<EventShortDto> findCompilationInfo(@RequestParam("ids") List<Long> eventIds);
}

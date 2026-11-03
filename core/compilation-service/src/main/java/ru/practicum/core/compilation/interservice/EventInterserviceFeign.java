package ru.practicum.core.compilation.interservice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.core.compilation.interservice.feign.EventFeignClient;
import ru.practicum.core.event.dto.response.EventShortDto;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventInterserviceFeign implements EventInterservice {
    private final EventFeignClient eventFeignClient;

    @Override
    public List<EventShortDto> findByIds(List<Long> eventIds) {
        return eventFeignClient.findCompilationInfo(eventIds);
    }
}

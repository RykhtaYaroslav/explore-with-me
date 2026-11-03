package ru.practicum.core.event.service.adminapi;

import ru.practicum.core.event.dto.EventQueryParams;
import ru.practicum.core.event.dto.request.UpdateEventAdminRequest;
import ru.practicum.core.event.dto.response.EventFullDto;

import java.util.List;

public interface EventServiceAdmin {
    List<EventFullDto> getAllFullInfoWithParams(EventQueryParams params);

    EventFullDto updateEvent(Long eventId, UpdateEventAdminRequest request);
}

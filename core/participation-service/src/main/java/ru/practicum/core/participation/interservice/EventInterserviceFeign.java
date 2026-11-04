package ru.practicum.core.participation.interservice;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.core.common.exception.ConflictException;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.event.dto.response.EventDtoForParticipationService;
import ru.practicum.core.participation.interservice.feign.EventFeignClient;

@RequiredArgsConstructor
@Service
public class EventInterserviceFeign implements EventInterservice {
    private final EventFeignClient eventFeignClient;

    @Override
    public void ensureOrganizerOrThrow(Long eventId, Long userId) {
        findByIdForOrganizerOrThrow(eventId, userId);
    }

    @Override
    public EventDtoForParticipationService findByIdForOrganizerOrThrow(Long eventId, Long userId) {
        EventDtoForParticipationService event = findByIdOrThrow(eventId);
        if (!event.initiatorId().equals(userId)) {
            throw new ConflictException("Only the event initiator can manage participation requests");
        }
        return event;
    }

    @Override
    public EventDtoForParticipationService findByIdOrThrow(Long eventId) {
        try {
            return eventFeignClient.findByIdOrThrow(eventId);
        } catch (FeignException e) {
            switch (e.status()) {
                case 404 -> throw new NotFoundException(String.format("Event with id = %d was not found", eventId));
                case 409 ->
                        throw new ConflictException(String.format("Conflict with business rules for event with id = %d", eventId));
                default -> throw e;
            }
        }

    }

    @Override
    public void synchronizeConfirmedRequestsOrThrow(Long eventId, long expectedCount, long count) {
        try {
            eventFeignClient.synchronizeConfirmedRequests(eventId, expectedCount, count);
        } catch (FeignException e) {
            switch (e.status()) {
                case 404 -> throw new NotFoundException(String.format("Event with id = %d was not found", eventId));
                case 409 -> throw new ConflictException(
                        String.format("Confirmed request count for event with id = %d conflicts with event capacity", eventId));
                default -> throw e;
            }
        }
    }
}

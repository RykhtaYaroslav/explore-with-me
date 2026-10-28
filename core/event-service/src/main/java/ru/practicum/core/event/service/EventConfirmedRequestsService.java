package ru.practicum.core.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.core.common.exception.ConflictException;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.event.repository.EventRepository;

@Service
@RequiredArgsConstructor
public class EventConfirmedRequestsService {
    private final EventRepository eventRepository;

    @Transactional
    public void adjustCount(Long eventId, long delta) {
        if (delta == 0) {
            throw new ConflictException("Change to confirmed request count must not be zero");
        }

        int updated = eventRepository.adjustConfirmedRequests(eventId, delta);
        if (updated == 1) {
            return;
        }

        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException(String.format("Event with id = %d was not found", eventId));
        }

        throw new ConflictException("Confirmed request count would exceed event capacity or become negative");
    }
}

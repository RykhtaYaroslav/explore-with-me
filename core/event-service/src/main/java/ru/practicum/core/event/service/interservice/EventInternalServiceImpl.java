package ru.practicum.core.event.service.interservice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.core.common.exception.ConflictException;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.event.dto.mapper.EventMapper;
import ru.practicum.core.event.dto.response.EventForParticipationServiceDto;
import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.core.event.model.Event;
import ru.practicum.core.event.repository.EventRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventInternalServiceImpl implements EventInternalService {
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final UserInterservice userInterservice;

    @Override
    public boolean existsById(Long eventId) {
        return eventRepository.existsById(eventId);
    }

    @Override
    public boolean isOrganizer(Long eventId, Long userId) {
        return eventRepository.existsByIdAndInitiatorId(eventId, userId);
    }

    @Override
    public EventShortDto getReviewInfo(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Event with id = %d was not found", eventId)));
        EventShortDto eventDto = eventMapper.toShortDto(event, 0L, event.getConfirmedRequests(), null);
        eventDto.setInitiator(userInterservice.getShortById(event.getInitiatorId()));
        return eventDto;
    }

    @Override
    public EventForParticipationServiceDto getParticipationInfoOrThrow(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Event with id = %d was not found", eventId)));

        return new EventForParticipationServiceDto(
                event.getId(),
                event.getState(),
                event.getEventDate(),
                event.getInitiatorId(),
                event.getParticipantLimit(),
                event.getRequestModeration(),
                event.getConfirmedRequests()
        );
    }

    @Override
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

        throw new ConflictException("Confirmed request count would exceed eventId capacity or become negative");
    }

    @Override
    @Transactional
    public void synchronizeConfirmedRequests(Long eventId, long expectedCount, long count) {
        if (expectedCount < 0 || count < 0) {
            throw new ConflictException("Confirmed request counts must not be negative");
        }

        int updated = eventRepository.setConfirmedRequests(eventId, expectedCount, count);
        if (updated == 1) {
            return;
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Event with id = %d was not found", eventId)));

        if (event.getConfirmedRequests() == count) {
            return;
        }

        if (event.getConfirmedRequests() != expectedCount) {
            throw new ConflictException("Confirmed request count changed during synchronization; retry the operation");
        }

        throw new ConflictException("Confirmed request count would exceed event capacity");
    }
}

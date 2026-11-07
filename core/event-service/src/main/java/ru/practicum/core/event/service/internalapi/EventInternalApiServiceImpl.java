package ru.practicum.core.event.service.internalapi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.core.common.exception.ConflictException;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.event.dto.mapper.EventMapper;
import ru.practicum.core.event.dto.response.EventDtoForParticipationService;
import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.core.event.model.Event;
import ru.practicum.core.event.repository.EventRepository;
import ru.practicum.core.event.service.EventStatsCollector;
import ru.practicum.core.event.service.interservice.UserInterservice;
import ru.practicum.core.user.dto.UserShortDto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EventInternalApiServiceImpl implements EventInternalApiService {
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final UserInterservice userInterservice;
    private final EventStatsCollector eventStatsCollector;

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
        UserShortDto initiator = userInterservice.getShortById(event.getInitiatorId());
        double weight = eventStatsCollector.getInteractionWeights(List.of(eventId)).getOrDefault(eventId, 0.0);
        return eventMapper.toShortDto(event, initiator, 0L, event.getConfirmedRequests(), null, weight);
    }

    @Override
    public List<EventShortDto> getReviewInfoByIds(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }

        List<Event> events = eventRepository.findAllWithCategoryByIdIn(eventIds);
        Map<Long, Event> eventsById = events.stream()
                .collect(Collectors.toMap(Event::getId, event -> event));
        List<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .toList();
        Map<Long, UserShortDto> usersById = userInterservice.getShortByIds(initiatorIds).stream()
                .collect(Collectors.toMap(UserShortDto::getId, user -> user));
        Map<Long, EventShortDto> eventDtosById = new LinkedHashMap<>();
        Map<Long, Double> weights = eventStatsCollector.getInteractionWeights(events.stream().map(Event::getId).toList());
        for (Long eventId : eventIds.stream().distinct().toList()) {
            Event event = eventsById.get(eventId);
            if (event == null) {
                throw new NotFoundException(String.format("Event with id = %d was not found", eventId));
            }
            eventDtosById.put(eventId, eventMapper.toShortDto(
                    event, usersById.get(event.getInitiatorId()), 0L, event.getConfirmedRequests(), null,
                    weights.getOrDefault(eventId, 0.0)));
        }

        return eventIds.stream()
                .distinct()
                .map(eventDtosById::get)
                .toList();
    }

    @Override
    public List<EventShortDto> getCompilationInfo(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }

        List<Event> events = eventRepository.findAllWithCategoryByIdIn(eventIds);
        if (events.isEmpty()) {
            return List.of();
        }

        List<EventShortDto> eventDtos = eventStatsCollector.getShortDtoListWithStats(events);
        Map<Long, EventShortDto> eventDtosById = new LinkedHashMap<>();
        eventDtos.forEach(eventDto -> eventDtosById.put(eventDto.getId(), eventDto));

        return eventIds.stream()
                .distinct()
                .filter(eventDtosById::containsKey)
                .map(eventDtosById::get)
                .toList();
    }

    @Override
    public EventDtoForParticipationService getParticipationInfoOrThrow(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Event with id = %d was not found", eventId)));

        return new EventDtoForParticipationService(
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
        log.info("Received confirmed request count adjustment: eventId={}, delta={}", eventId, delta);
        if (delta == 0) {
            throw new ConflictException("Change to confirmed request count must not be zero");
        }

        int updated = eventRepository.adjustConfirmedRequests(eventId, delta);
        if (updated == 1) {
            Long confirmedRequests = eventRepository.findById(eventId)
                    .map(Event::getConfirmedRequests)
                    .orElse(null);
            log.info("Confirmed request count adjustment applied: eventId={}, delta={}, updatedRows={}, confirmedRequests={}",
                    eventId, delta, updated, confirmedRequests);
            return;
        }

        log.warn("Confirmed request count adjustment did not update an event: eventId={}, delta={}, updatedRows={}",
                eventId, delta, updated);

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

package ru.practicum.core.participation.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.core.common.enums.RequestStatus;
import ru.practicum.core.common.exception.ConflictException;
import ru.practicum.core.event.dto.response.EventForParticipationServiceDto;
import ru.practicum.core.event.model.EventState;
import ru.practicum.core.participation.interservice.EventInterservice;
import ru.practicum.core.participation.interservice.UserInterservice;
import ru.practicum.core.participation.model.ParticipationRequest;
import ru.practicum.core.participation.repository.RequestRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class RequestValidatorImpl implements RequestValidator {
    private static final String OUT_OF_LIMIT_MESSAGE = "На это событие больше нет свободных мест";

    private final UserInterservice userInterservice;
    private final EventInterservice eventInterservice;
    private final RequestRepository requestRepository;

    @Override
    public void validateUserExists(Long userId) {
        userInterservice.existsByIdOrThrow(userId);
    }

    @Override
    public RequestStatus validateCreateRequestAndGetStatus(Long userId, Long eventId) {
        validateUserExists(userId);
        EventForParticipationServiceDto event = eventInterservice.findByIdOrThrow(eventId);

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Вы уже подали заявку на участие в этом событии!");
        }

        if (event.initiatorId().equals(userId)) {
            throw new ConflictException("Организатор события не может подать заявку на участие в нём!");
        }

        if (event.state() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя подать заявку на неопубликованное событие!");
        }

        if (!event.eventDate().isAfter(LocalDateTime.now())) {
            throw new ConflictException("Нельзя подать заявку на уже начавшееся событие!");
        }

        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        Long eventConfirmedCount = event.confirmedRequests();
        if (eventConfirmedCount == null) {
            throw new ConflictException("Event response does not contain confirmed request count");
        }
        if (confirmedCount != eventConfirmedCount) {
            eventInterservice.synchronizeConfirmedRequestsOrThrow(
                    eventId, eventConfirmedCount, confirmedCount);
        }

        int participantLimit = event.participantLimit();
        if (participantLimit > 0 && confirmedCount >= participantLimit) {
            throw new ConflictException(OUT_OF_LIMIT_MESSAGE);
        }

        if (participantLimit == 0 || !event.requestModeration()) {
            return RequestStatus.CONFIRMED;
        }
        return RequestStatus.PENDING;
    }

    @Override
    public void validateCancelRequest(Long userId, ParticipationRequest request) {
        if (!request.getRequesterId().equals(userId)) {
            throw new ConflictException("Вы не можете отменить чужую заявку на участие!");
        }
    }
}

package ru.practicum.main.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.core.common.enums.RequestStatus;
import ru.practicum.core.event.model.EventState;
import ru.practicum.core.user.model.User;
import ru.practicum.core.user.repository.UserRepository;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.request.client.EventFeignClient;
import ru.practicum.main.request.dto.ParticipationRequestDto;
import ru.practicum.main.request.dto.mapper.RequestMapper;
import ru.practicum.main.request.model.ParticipationRequest;
import ru.practicum.main.request.repository.RequestRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestMapper requestMapper;
    private final EventFeignClient eventFeignClient;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден!");
        }

        return requestRepository.findAllByRequesterId(userId).stream()
                .map(requestMapper::toDtoOut)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {

        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + userId + " не найден!"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id = " + eventId + " не найдено!"));

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Вы уже подали заявку на участие в этом событии!");
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Организатор события не может подать заявку на участие в нём!");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя подать заявку на неопубликованное событие!");
        }

        RequestStatus startStatus = RequestStatus.PENDING;

        Integer limit = event.getParticipantLimit();
        if (limit > 0) {
            long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (confirmedCount >= limit) {
                throw new ConflictException("На это событие больше нет свободных мест!");
            }
        }
        if (limit == 0 || !event.getRequestModeration()) {
            startStatus = RequestStatus.CONFIRMED;
        } else {

            long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);

            if (confirmedCount >= limit) {
                throw new ConflictException("На это событие больше нет свободных мест!");
            }
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .created(java.time.LocalDateTime.now())
                .event(event)
                .requester(requester)
                .status(startStatus)
                .build();

        ParticipationRequest savedRequest = requestRepository.save(request);
        if (startStatus == RequestStatus.CONFIRMED) {
            eventFeignClient.adjustConfirmedRequestCount(eventId, 1);
        }
        return requestMapper.toDtoOut(savedRequest);
    }


    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Заявка с id = " + requestId + " не найдена!"));

        if (!request.getRequester().getId().equals(userId)) {
            throw new ConflictException("Вы не можете отменить чужую заявку на участие!");
        }

        boolean wasConfirmed = request.getStatus() == RequestStatus.CONFIRMED;
        request.setStatus(RequestStatus.CANCELED);
        requestRepository.save(request);
        if (wasConfirmed) {
            eventFeignClient.adjustConfirmedRequestCount(request.getEvent().getId(), -1);
        }

        return requestMapper.toDtoOut(request);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isConfirmedParticipant(Long userId, Long eventId) {
        return requestRepository.existsByRequesterIdAndEventIdAndStatus(
                userId, eventId, RequestStatus.CONFIRMED
        );
    }

}

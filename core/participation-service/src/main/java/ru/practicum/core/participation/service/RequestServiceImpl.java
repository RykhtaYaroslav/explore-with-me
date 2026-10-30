package ru.practicum.core.participation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.core.common.enums.RequestStatus;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.participation.client.EventFeignClient;
import ru.practicum.core.participation.dto.ParticipationRequestDto;
import ru.practicum.core.participation.dto.mapper.RequestMapper;
import ru.practicum.core.participation.model.ParticipationRequest;
import ru.practicum.core.participation.repository.RequestRepository;
import ru.practicum.core.participation.validation.RequestValidator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final RequestValidator requestValidator;
    private final RequestMapper requestMapper;
    private final EventFeignClient eventFeignClient;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        requestValidator.validateUserExists(userId);

        return requestRepository.findAllByRequesterId(userId).stream()
                .map(requestMapper::toDtoOut)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        RequestStatus startStatus = requestValidator.validateCreateRequestAndGetStatus(userId, eventId);

        ParticipationRequest request = ParticipationRequest.builder()
                .created(LocalDateTime.now())
                .eventId(eventId)
                .requesterId(userId)
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

        requestValidator.validateCancelRequest(userId, request);

        boolean wasConfirmed = request.getStatus() == RequestStatus.CONFIRMED;
        request.setStatus(RequestStatus.CANCELED);
        requestRepository.save(request);
        if (wasConfirmed) {
            eventFeignClient.adjustConfirmedRequestCount(request.getEventId(), -1);
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

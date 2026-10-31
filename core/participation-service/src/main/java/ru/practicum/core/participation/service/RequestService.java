package ru.practicum.core.participation.service;

import ru.practicum.core.participation.dto.EventRequestStatusUpdateRequest;
import ru.practicum.core.participation.dto.EventRequestStatusUpdateResult;
import ru.practicum.core.participation.dto.ParticipationRequestDto;

import java.util.List;

public interface RequestService {

    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto createRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getEventRequestsByInitiator(Long userId, Long eventId);

    EventRequestStatusUpdateResult changeRequestsStatus(Long userId, Long eventId,
                                                        EventRequestStatusUpdateRequest update);

    boolean isConfirmedParticipant(Long userId, Long eventId);

}

package ru.practicum.core.participation.validation;

import ru.practicum.core.common.enums.RequestStatus;
import ru.practicum.core.participation.dto.EventRequestStatusUpdateRequest;
import ru.practicum.core.participation.dto.internal.RequestStatusUpdateContext;
import ru.practicum.core.participation.model.ParticipationRequest;

public interface RequestValidator {
    void validateUserExists(Long userId);

    RequestStatus validateCreateRequestAndGetStatus(Long userId, Long eventId);

    void validateCancelRequest(Long userId, ParticipationRequest request);

    void validateEventRequestsAccess(Long userId, Long eventId);

    RequestStatusUpdateContext validateChangeRequestsStatus(Long userId, Long eventId,
                                                            EventRequestStatusUpdateRequest update);
}

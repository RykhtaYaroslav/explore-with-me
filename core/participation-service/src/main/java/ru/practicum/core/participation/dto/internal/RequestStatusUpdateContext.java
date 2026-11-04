package ru.practicum.core.participation.dto.internal;

import ru.practicum.core.common.enums.RequestStatus;
import ru.practicum.core.participation.model.ParticipationRequest;

import java.util.List;

/**
 * Carries validated request data from the validator to the request service.
 *
 * @param status                requested target status
 * @param requests              pending requests selected for the update
 * @param confirmedRequestCount number of requests already confirmed for the event
 * @param participantLimit      maximum number of confirmed participants, or zero when unlimited
 */
public record RequestStatusUpdateContext(
        RequestStatus status,
        List<ParticipationRequest> requests,
        long confirmedRequestCount,
        int participantLimit
) {
}

package ru.practicum.core.event.service.interservice;

import ru.practicum.core.common.dto.participation.ConfirmedRequestsCount;
import ru.practicum.core.common.enums.RequestStatus;

import java.util.List;

public interface ParticipationRequestInterservice {
    List<ConfirmedRequestsCount> countRequestsCountByEventIds (List<Long> eventIds, RequestStatus status);

}

package ru.practicum.core.event.service.interservice;

import ru.practicum.core.common.dto.participation.ConfirmedRequestsCount;
import ru.practicum.core.common.enums.RequestStatus;

import java.util.List;

public class ParticipationRequestInterserviceFeign implements ParticipationRequestInterservice {
    @Override
    public List<ConfirmedRequestsCount> countRequestsCountByEventIds(List<Long> eventIds, RequestStatus status) {
        //Todo: implement this
        return List.of();
    }
}

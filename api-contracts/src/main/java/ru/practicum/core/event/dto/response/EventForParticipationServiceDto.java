package ru.practicum.core.event.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import ru.practicum.core.event.model.EventState;

import java.time.LocalDateTime;

import static ru.practicum.core.common.util.EwmConstants.DATE_TIME_FORMAT;
// TODO rename
public record EventForParticipationServiceDto(
        Long id,
        EventState state,

        @JsonFormat(pattern = DATE_TIME_FORMAT)
        LocalDateTime eventDate,

        Long initiatorId,
        Integer participantLimit,
        Boolean requestModeration,
        Long confirmedRequests
) {
}

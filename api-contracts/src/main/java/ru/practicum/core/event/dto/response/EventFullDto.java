package ru.practicum.core.event.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.core.category.dto.CategoryDto;
import ru.practicum.core.common.dto.user.UserShortDto;
import ru.practicum.core.event.dto.LocationDto;
import ru.practicum.core.event.model.EventState;

import java.time.LocalDateTime;

import static ru.practicum.core.common.util.EwmConstants.DATE_TIME_FORMAT;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventFullDto {
    private Long id;
    private String title;
    private String annotation;
    private String description;
    private CategoryDto category;

    @JsonFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime createdOn;

    @JsonFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime eventDate;

    @JsonFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime publishedOn;

    private UserShortDto initiator;
    private LocationDto location;
    private Boolean paid;
    private Integer participantLimit;
    private Boolean requestModeration;
    private EventState state;
    private Long confirmedRequests;
    private Long views;
    private Double rating;
}

package ru.practicum.core.event.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.core.event.category.dto.CategoryDto;
import ru.practicum.core.user.dto.UserShortDto;

import java.time.LocalDateTime;

import static ru.practicum.core.common.util.EwmConstants.DATE_TIME_FORMAT;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class EventShortDto {
    private Long id;
    private String title;
    private String description;
    private String annotation;
    private CategoryDto category;
    private Long confirmedRequests;

    @JsonFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime eventDate;

    private UserShortDto initiator;
    private Boolean paid;
    private Long views;
    private Double rating;
    private double totalInteractionWeight;
}

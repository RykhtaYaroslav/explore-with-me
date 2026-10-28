package ru.practicum.core.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.core.common.dto.user.UserShortDto;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutputUserReviewDto {
    private UserShortDto rater;
    private UserShortDto target;
    private EventShortDto event;
    private Integer score;
    private String comment;
}

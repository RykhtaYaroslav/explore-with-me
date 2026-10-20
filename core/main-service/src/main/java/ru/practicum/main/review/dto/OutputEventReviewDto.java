package ru.practicum.main.review.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.main.user.dto.UserShortDto;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutputEventReviewDto {
    private UserShortDto rater;
    private UserShortDto event;
    private Integer score;
    private String comment;
}
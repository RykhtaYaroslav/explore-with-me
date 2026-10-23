package ru.practicum.main.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static ru.practicum.main.util.EwmConstants.MAX_REVIEW_COMMENT_LENGTH;
import static ru.practicum.main.util.EwmConstants.MAX_REVIEW_SCORE;
import static ru.practicum.main.util.EwmConstants.MIN_REVIEW_SCORE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewUserReviewDto {

    @NotNull(message = "Review score can't be empty")
    @Min(value = MIN_REVIEW_SCORE, message = "Score must be from {value} to " + MAX_REVIEW_SCORE)
    @Max(value = MAX_REVIEW_SCORE, message = "Score must be from " + MIN_REVIEW_SCORE + " to {value}")
    private Integer score;

    @Size(max = MAX_REVIEW_COMMENT_LENGTH)
    private String comment;
}

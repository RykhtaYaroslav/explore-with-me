package ru.practicum.main.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.main.util.validation.FutureAfterHours;

import java.time.LocalDateTime;

import static ru.practicum.main.util.EwmConstants.DATE_TIME_FORMAT;
import static ru.practicum.main.util.EwmConstants.MAX_ANNOT_LENGTH;
import static ru.practicum.main.util.EwmConstants.MAX_DESC_LENGTH;
import static ru.practicum.main.util.EwmConstants.MAX_TITLE_LENGTH;
import static ru.practicum.main.util.EwmConstants.MIN_ANNOTATION_LENGTH;
import static ru.practicum.main.util.EwmConstants.MIN_DESCRIPTION_LENGTH;
import static ru.practicum.main.util.EwmConstants.MIN_TITLE_LENGTH;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class NewEventDto {
    @NotBlank(message = "Event title must not be blank")
    @Size(min = MIN_TITLE_LENGTH, max = MAX_TITLE_LENGTH,
            message = "Title length must be from {min} to {max}")
    private String title;

    @NotBlank(message = "Event annotation must not be blank")
    @Size(min = MIN_ANNOTATION_LENGTH, max = MAX_ANNOT_LENGTH,
            message = "Annotation length must be from {min} to {max}")
    private String annotation;

    @NotBlank(message = "Event description must not be blank")
    @Size(
            min = MIN_DESCRIPTION_LENGTH,
            max = MAX_DESC_LENGTH,
            message = "Description length must be from {min} to {max}")
    private String description;

    @NotNull(message = "Category ID must not be null")
    @JsonProperty("category")
    private Long categoryId;

    @NotNull(message = "Event date must not be null")
    @FutureAfterHours(hours = 2, message = "Event date cannot be earlier than {hours} hours from now")
    @JsonFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime eventDate;

    @NotNull
    @Valid
    private LocationDto location;

    @Builder.Default
    private Boolean paid = false;

    @PositiveOrZero(message = "Participant limit must be positive or zero")
    @Builder.Default
    private Integer participantLimit = 0;

    @Builder.Default
    private Boolean requestModeration = true;
}

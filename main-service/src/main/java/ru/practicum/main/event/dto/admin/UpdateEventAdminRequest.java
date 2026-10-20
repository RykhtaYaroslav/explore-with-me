package ru.practicum.main.event.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.main.event.dto.LocationDto;
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventAdminRequest {

    @Size(min = MIN_TITLE_LENGTH, max = MAX_TITLE_LENGTH,
            message = "Title length must be from {min} to {max}")
    private String title;

    @Size(min = MIN_ANNOTATION_LENGTH, max = MAX_ANNOT_LENGTH,
            message = "Annotation length must be from {min} to {max}")
    private String annotation;

    @Size(min = MIN_DESCRIPTION_LENGTH, max = MAX_DESC_LENGTH,
            message = "Description length must be from {min} to {max}")
    private String description;

    @JsonProperty("category")
    private Long categoryId;

    @FutureAfterHours(hours = 1, message = "Event date cannot be earlier than {hours} hour from now")
    @JsonFormat(pattern = DATE_TIME_FORMAT)
    private LocalDateTime eventDate;

    @Valid
    private LocationDto location;

    private Boolean paid;

    @PositiveOrZero(message = "Participant limit must be positive or zero")
    private Integer participantLimit;

    private Boolean requestModeration;

    private StateActionAdmin stateAction;
}
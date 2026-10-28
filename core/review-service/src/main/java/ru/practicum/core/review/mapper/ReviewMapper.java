package ru.practicum.core.review.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.core.common.dto.user.UserShortDto;
import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.core.review.dto.NewEventReviewDto;
import ru.practicum.core.review.dto.NewUserReviewDto;
import ru.practicum.core.review.dto.OutputEventReviewDto;
import ru.practicum.core.review.dto.OutputUserReviewDto;
import ru.practicum.core.review.model.EventReview;
import ru.practicum.core.review.model.UserReview;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "raterId", source = "raterId")
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "score", source = "request.score")
    @Mapping(target = "comment", source = "request.comment")
    EventReview toEventReview(NewEventReviewDto request, Long raterId, Long eventId);

    @Mapping(target = "rater", source = "rater")
    @Mapping(target = "event", source = "event")
    @Mapping(target = "score", source = "review.score")
    @Mapping(target = "comment", source = "review.comment")
    OutputEventReviewDto toEventReviewOutDto(EventReview review, UserShortDto rater, EventShortDto event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "raterId", source = "raterId")
    @Mapping(target = "targetId", source = "targetId")
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "score", source = "request.score")
    @Mapping(target = "comment", source = "request.comment")
    UserReview toUserReview(NewUserReviewDto request, Long raterId, Long targetId, Long eventId);

    @Mapping(target = "rater", source = "rater")
    @Mapping(target = "target", source = "target")
    @Mapping(target = "event", source = "event")
    @Mapping(target = "score", source = "review.score")
    @Mapping(target = "comment", source = "review.comment")
    OutputUserReviewDto toUserReviewOutDto(
            UserReview review,
            UserShortDto rater,
            UserShortDto target,
            EventShortDto event
    );
}

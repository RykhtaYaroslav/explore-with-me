package ru.practicum.core.review.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.core.review.dto.NewEventReviewDto;
import ru.practicum.core.review.dto.NewUserReviewDto;
import ru.practicum.core.review.dto.OutputEventReviewDto;
import ru.practicum.core.review.dto.OutputUserReviewDto;
import ru.practicum.core.review.model.EventReview;
import ru.practicum.core.review.model.UserReview;
import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.main.event.model.Event;
import ru.practicum.core.common.dto.user.UserShortDto;
import ru.practicum.main.user.model.User;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    @Mapping(target = "rater", source = "rater")
    @Mapping(target = "eventId", source = "event")
    @Mapping(target = "score", source = "request.score")
    @Mapping(target = "comment", source = "request.comment")
    EventReview toEventReview(NewEventReviewDto request, User rater, Event event);

    @Mapping(target = "rater", source = "rater")
    @Mapping(target = "event", source = "event")
    @Mapping(target = "score", source = "eventReview.score")
    @Mapping(target = "comment", source = "eventReview.comment")
    OutputEventReviewDto toEventReviewOutDto(EventReview eventReview, UserShortDto rater, EventShortDto event);

    @Mapping(target = "rater", source = "rater")
    @Mapping(target = "targetId", source = "target")
    @Mapping(target = "eventId", source = "event")
    @Mapping(target = "score", source = "request.score")
    @Mapping(target = "comment", source = "request.comment")
    UserReview toUserReview(NewUserReviewDto request, User rater, User target, Event event);

    @Mapping(target = "rater", source = "rater")
    @Mapping(target = "target", source = "target")
    @Mapping(target = "event", source = "event")
    @Mapping(target = "score", source = "userReview.score")
    @Mapping(target = "comment", source = "userReview.comment")
    OutputUserReviewDto toUserReviewOutDto(UserReview userReview, UserShortDto rater, UserShortDto target, EventShortDto event);
}

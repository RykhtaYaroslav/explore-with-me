package ru.practicum.main.review.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.review.dto.NewEventReviewDto;
import ru.practicum.main.review.dto.NewUserReviewDto;
import ru.practicum.main.review.dto.OutputEventReviewDto;
import ru.practicum.main.review.dto.OutputUserReviewDto;
import ru.practicum.main.review.model.EventReview;
import ru.practicum.main.review.model.UserReview;
import ru.practicum.main.user.dto.UserShortDto;
import ru.practicum.main.user.model.User;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    @Mapping(target = "rater", source = "rater")
    @Mapping(target = "event", source = "event")
    @Mapping(target = "score", source = "request.score")
    @Mapping(target = "comment", source = "request.comment")
    EventReview toEventReview(NewEventReviewDto request, User rater, Event event);

    @Mapping(target = "rater", source = "rater")
    @Mapping(target = "event", source = "event")
    @Mapping(target = "score", source = "eventReview.score")
    @Mapping(target = "comment", source = "eventReview.comment")
    OutputEventReviewDto toEventReviewOutDto(EventReview eventReview, UserShortDto rater, EventShortDto event);

    @Mapping(target = "rater", source = "rater")
    @Mapping(target = "target", source = "target")
    @Mapping(target = "event", source = "event")
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

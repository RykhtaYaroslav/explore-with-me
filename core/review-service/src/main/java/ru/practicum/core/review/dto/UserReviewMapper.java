package ru.practicum.core.review.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.core.review.model.UserReview;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.user.model.User;

@Mapper(componentModel = "spring")
public interface UserReviewMapper {

    @Mapping(target = "rater", source = "rater")
    @Mapping(target = "targetId", source = "target")
    @Mapping(target = "eventId", source = "event")
    UserReview toUserReview(NewUserReviewDto dto, User rater, User target, Event event);

    OutputUserReviewDto toOutReviewDto(UserReview review);
}

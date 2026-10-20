package ru.practicum.main.review.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.review.model.UserReview;
import ru.practicum.main.user.model.User;

@Mapper(componentModel = "spring")
public interface UserReviewMapper {

    @Mapping(target = "rater", source = "rater")
    @Mapping(target = "target", source = "target")
    @Mapping(target = "event", source = "event")
    UserReview toUserReview(NewUserReviewDto dto, User rater, User target, Event event);

    OutputUserReviewDto toOutReviewDto(UserReview review);
}

package ru.practicum.main.review.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.event.service.EventStatsCollector;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.request.model.RequestStatus;
import ru.practicum.main.request.repository.RequestRepository;
import ru.practicum.main.review.dto.NewEventReviewDto;
import ru.practicum.main.review.dto.NewUserReviewDto;
import ru.practicum.main.review.dto.OutputEventReviewDto;
import ru.practicum.main.review.dto.OutputUserReviewDto;
import ru.practicum.main.review.dto.mapper.ReviewMapper;
import ru.practicum.main.review.model.EventReview;
import ru.practicum.main.review.model.UserReview;
import ru.practicum.main.review.repository.EventReviewRepository;
import ru.practicum.main.review.repository.UserReviewRepository;
import ru.practicum.main.time.TimeProvider;
import ru.practicum.main.user.dto.UserMapper;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.main.util.EwmConstants.HOURS_OFFSET_FOR_REVIEWS_CREATING;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final EventStatsCollector eventStatsCollector;

    private final UserReviewRepository userReviewRepository;
    private final EventReviewRepository eventReviewRepository;

    private final ReviewMapper reviewMapper;
    private final UserMapper userMapper;

    private final TimeProvider timeProvider;

    @Override
    public OutputUserReviewDto createAuthorReview(Long raterId, Long eventId, Long authorId, NewUserReviewDto request) {
        User rater = findUserOrThrow(raterId);
        User target = findUserOrThrow(authorId);
        Event event = findEventOrThrow(eventId);

        validateAuthorReviewRules(event, rater, target);

        UserReview review = reviewMapper.toUserReview(request, rater, target, event);

        review = userReviewRepository.save(review);

        return reviewMapper.toUserReviewOutDto(
                review,
                userMapper.toUserShortDto(rater),
                userMapper.toUserShortDto(target),
                eventStatsCollector.getShortDtoListWithStats(List.of(event)).getFirst()
        );
    }

    @Override
    public OutputEventReviewDto createEventReview(Long raterId, Long eventId, NewEventReviewDto request) {
        User rater = findUserOrThrow(raterId);
        Event event = findEventOrThrow(eventId);

        validateEventReviewRules(event, rater);

        EventReview review = reviewMapper.toEventReview(request, rater, event);

        review = eventReviewRepository.save(review);

        return reviewMapper.toEventReviewOutDto(
                review,
                userMapper.toUserShortDto(rater),
                eventStatsCollector.getShortDtoListWithStats(List.of(event)).getFirst()
        );
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId).orElseThrow(()
                -> new NotFoundException(String.format("User with id = %d not found", userId)));
    }

    private Event findEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id=%d not found", eventId)));
    }

    private void validateEventReviewRules(Event event, User rater) {
        Long eventId = event.getId();
        Long raterId = rater.getId();

        validateDateTime(event.getEventDate());

        validateParticipation(eventId, raterId);

        if (isReviewExist(eventId, raterId)) {
            String m = String.format("Пользователь с id = %d уже оставил оценку событию с id = %d", raterId, eventId);
            throw new ConflictException(m);
        }
    }

    private void validateAuthorReviewRules(Event event, User rater, User author) {
        Long eventId = event.getId();
        Long raterId = rater.getId();
        Long authorId = author.getId();

        validateDateTime(event.getEventDate());

        if (!event.getInitiator().getId().equals(authorId)) {
            throw new ConflictException("Юзер с id = " + authorId + " не является создателем данного события");
        }

        validateParticipation(eventId, raterId);

        if (isReviewExist(eventId, raterId, authorId)) {
            String m = String.format("Пользователь с id = %d уже ставил оценку автору с id = %d на мероприятии с id = %d", raterId, authorId, eventId);
            throw new ConflictException(m);
        }
    }

    private void validateDateTime(LocalDateTime eventDate) {
        LocalDateTime allowedTime = eventDate.plusHours(HOURS_OFFSET_FOR_REVIEWS_CREATING);

        if (timeProvider.now().isBefore(allowedTime)) {
            String m = String.format("Оставить оценку событию можно только спустя %d часов после начала", HOURS_OFFSET_FOR_REVIEWS_CREATING);
            throw new ConflictException(m);
        }
    }

    private void validateParticipation(Long eventId, Long userId) {
        boolean isParticipant = requestRepository.existsByRequesterIdAndEventIdAndStatus(
                userId, eventId, RequestStatus.CONFIRMED
        );

        if (!isParticipant) {
            String m = String.format("Пользователь с id = %d не является участником события с id = %d", userId, eventId);
            throw new ConflictException(m);
        }
    }

    private boolean isReviewExist(Long eventId, Long raterId, Long authorId) {
        return userReviewRepository.existsByParams(raterId, eventId, authorId);
    }

    private boolean isReviewExist(Long eventId, Long raterId) {
        return eventReviewRepository.existsByParams(raterId, eventId);
    }
}

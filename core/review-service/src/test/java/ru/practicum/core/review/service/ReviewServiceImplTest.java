package ru.practicum.core.review.service;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.core.review.dto.NewEventReviewDto;
import ru.practicum.core.review.dto.OutputEventReviewDto;
import ru.practicum.core.review.interservice.EventInterservice;
import ru.practicum.core.review.interservice.UserInterservice;
import ru.practicum.core.review.mapper.ReviewMapper;
import ru.practicum.core.review.model.EventReview;
import ru.practicum.core.review.repository.EventReviewRepository;
import ru.practicum.core.review.repository.UserReviewRepository;
import ru.practicum.core.review.util.TestDataUtils;
import ru.practicum.core.review.validation.ReviewValidator;
import ru.practicum.core.user.dto.UserShortDto;
import ru.practicum.ewm.stats.model.action.ActionType;
import ru.practicum.ewm.stats.model.action.UserAction;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {
    @Mock
    private ReviewValidator validator;
    @Mock
    private EventInterservice events;
    @Mock
    private UserInterservice users;
    @Mock
    private UserReviewRepository userReviews;
    @Mock
    private EventReviewRepository eventReviews;
    @Mock
    private ReviewMapper mapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    private ReviewServiceImpl service;
    private EasyRandom random;

    @BeforeEach
    void setUp() {
        service = new ReviewServiceImpl(validator, events, users, userReviews, eventReviews, mapper, eventPublisher);
        random = TestDataUtils.createEasyRandomizer();
    }

    @AfterEach
    void verifyNoMoreMockInteractions() {
        verifyNoMoreInteractions(validator, events, users, userReviews, eventReviews, mapper, eventPublisher);
    }

    @Test
    @DisplayName("Создает отзыв о событии для пользователя, прошедшего проверку доступности")
    void createsEventReview() {
        UserShortDto rater = user(7L);
        EventShortDto event = event(15L);
        NewEventReviewDto request = new NewEventReviewDto(5, "Great event");
        EventReview entity = EventReview.builder().raterId(7L).eventId(15L).score(5).comment("Great event").build();
        EventReview saved = EventReview.builder().id(99L).raterId(7L).eventId(15L).score(5).comment("Great event").build();
        OutputEventReviewDto output = random.nextObject(OutputEventReviewDto.class);
        when(validator.findUserOrThrow(7L)).thenReturn(rater);
        when(validator.findEventOrThrow(15L)).thenReturn(event);
        when(mapper.toEventReview(request, 7L, 15L)).thenReturn(entity);
        when(eventReviews.save(entity)).thenReturn(saved);
        when(mapper.toEventReviewOutDto(saved, rater, event)).thenReturn(output);

        assertThat(service.createEventReview(7L, 15L, request)).isSameAs(output);
        verify(validator, times(1)).validateEventReview(7L, event);
        verify(eventReviews, times(1)).save(entity);
        verify(eventPublisher, times(1)).publishEvent(argThat((UserAction action) ->
                action.userId() == 7L && action.eventId() == 15L && action.actionType() == ActionType.LIKE));
    }

    @Test
    @DisplayName("Возвращает отзывы события в порядке репозитория и загружает авторов без дубликатов")
    void getsEventReviewsInRepositoryOrder() {
        EventShortDto event = event(15L);
        EventReview newer = EventReview.builder().id(12L).eventId(15L).raterId(7L).score(5).build();
        EventReview older = EventReview.builder().id(8L).eventId(15L).raterId(7L).score(3).build();
        UserShortDto rater = user(7L);
        OutputEventReviewDto newerDto = random.nextObject(OutputEventReviewDto.class);
        OutputEventReviewDto olderDto = random.nextObject(OutputEventReviewDto.class);
        when(validator.findEventOrThrow(15L)).thenReturn(event);
        when(eventReviews.findAllByEventIdOrderByIdDesc(15L)).thenReturn(List.of(newer, older));
        when(users.getShortByIds(List.of(7L))).thenReturn(List.of(rater));
        when(mapper.toEventReviewOutDto(newer, rater, event)).thenReturn(newerDto);
        when(mapper.toEventReviewOutDto(older, rater, event)).thenReturn(olderDto);

        assertThat(service.getEventReviews(15L)).containsExactly(newerDto, olderDto);
        verify(users, times(1)).getShortByIds(List.of(7L));
        InOrder order = inOrder(mapper);
        order.verify(mapper, times(1)).toEventReviewOutDto(newer, rater, event);
        order.verify(mapper, times(1)).toEventReviewOutDto(older, rater, event);
    }

    @Test
    @DisplayName("Пустой список отзывов события не вызывает межсервисный поиск пользователей")
    void emptyEventReviewsSkipUserLookup() {
        when(validator.findEventOrThrow(15L)).thenReturn(event(15L));
        when(eventReviews.findAllByEventIdOrderByIdDesc(15L)).thenReturn(List.of());

        assertThat(service.getEventReviews(15L)).isEmpty();

        verifyNoInteractions(users);
    }

    @Test
    @DisplayName("Отзывы организатору разрешают авторов и события одним запросом без дубликатов")
    void getsAuthorReviewsWithDeduplicatedRelations() {
        UserShortDto author = user(20L);
        UserShortDto rater = user(7L);
        EventShortDto firstEvent = event(31L);
        EventShortDto secondEvent = event(32L);
        UserReviewFixture fixture = userReviews();
        when(validator.findUserOrThrow(20L)).thenReturn(author);
        when(userReviews.findAllByTargetIdOrderByIdDesc(20L)).thenReturn(fixture.reviews);
        when(users.getShortByIds(List.of(7L))).thenReturn(List.of(rater));
        when(events.getReviewInfoByIds(List.of(31L, 32L))).thenReturn(List.of(firstEvent, secondEvent));
        when(mapper.toUserReviewOutDto(fixture.reviews.get(0), rater, author, firstEvent)).thenReturn(fixture.firstDto);
        when(mapper.toUserReviewOutDto(fixture.reviews.get(1), rater, author, firstEvent)).thenReturn(fixture.secondDto);
        when(mapper.toUserReviewOutDto(fixture.reviews.get(2), author, author, secondEvent)).thenReturn(fixture.thirdDto);

        assertThat(service.getAuthorReviews(20L)).containsExactly(fixture.firstDto, fixture.secondDto, fixture.thirdDto);
        verify(users, times(1)).getShortByIds(List.of(7L));
        verify(events, times(1)).getReviewInfoByIds(List.of(31L, 32L));
        verify(mapper, times(1)).toUserReviewOutDto(fixture.reviews.get(0), rater, author, firstEvent);
        verify(mapper, times(1)).toUserReviewOutDto(fixture.reviews.get(1), rater, author, firstEvent);
        verify(mapper, times(1)).toUserReviewOutDto(fixture.reviews.get(2), author, author, secondEvent);
    }

    private UserReviewFixture userReviews() {
        var first = ru.practicum.core.review.model.UserReview.builder().id(30L).targetId(20L).raterId(7L).eventId(31L).build();
        var second = ru.practicum.core.review.model.UserReview.builder().id(25L).targetId(20L).raterId(7L).eventId(31L).build();
        var third = ru.practicum.core.review.model.UserReview.builder().id(16L).targetId(20L).raterId(20L).eventId(32L).build();
        return new UserReviewFixture(List.of(first, second, third),
                random.nextObject(ru.practicum.core.review.dto.OutputUserReviewDto.class),
                random.nextObject(ru.practicum.core.review.dto.OutputUserReviewDto.class),
                random.nextObject(ru.practicum.core.review.dto.OutputUserReviewDto.class));
    }

    private UserShortDto user(Long id) {
        UserShortDto user = random.nextObject(UserShortDto.class);
        user.setId(id);
        return user;
    }

    private EventShortDto event(Long id) {
        EventShortDto event = random.nextObject(EventShortDto.class);
        event.setId(id);
        return event;
    }

    private record UserReviewFixture(List<ru.practicum.core.review.model.UserReview> reviews,
                                     ru.practicum.core.review.dto.OutputUserReviewDto firstDto,
                                     ru.practicum.core.review.dto.OutputUserReviewDto secondDto,
                                     ru.practicum.core.review.dto.OutputUserReviewDto thirdDto) {
    }
}

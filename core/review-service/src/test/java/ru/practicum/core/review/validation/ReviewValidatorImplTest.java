package ru.practicum.core.review.validation;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.core.common.exception.ConflictException;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.common.time.TimeProvider;
import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.core.review.interservice.EventInterservice;
import ru.practicum.core.review.interservice.ParticipationInterservice;
import ru.practicum.core.review.interservice.UserInterservice;
import ru.practicum.core.review.repository.EventReviewRepository;
import ru.practicum.core.review.repository.UserReviewRepository;
import ru.practicum.core.review.util.TestDataUtils;
import ru.practicum.core.user.dto.UserShortDto;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewValidatorImplTest {
    private static final long RATER_ID = 7L;
    private static final long EVENT_ID = 15L;
    private static final long AUTHOR_ID = 21L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 12, 0);
    @Mock
    private UserInterservice users;
    @Mock
    private EventInterservice events;
    @Mock
    private ParticipationInterservice participation;
    @Mock
    private UserReviewRepository userReviews;
    @Mock
    private EventReviewRepository eventReviews;
    private ReviewValidatorImpl validator;
    private EasyRandom random;

    @BeforeEach
    void setUp() {
        validator = new ReviewValidatorImpl(users, events, participation, userReviews, eventReviews,
                new TimeProvider(Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)));
        random = TestDataUtils.createEasyRandomizer();
    }

    @AfterEach
    void verifyNoMoreMockInteractions() {
        verifyNoMoreInteractions(users, events, participation, userReviews, eventReviews);
    }

    @Test
    @DisplayName("Находит существующих пользователя и событие через межсервисные клиенты")
    void resolvesExistingEntities() {
        UserShortDto user = random.nextObject(UserShortDto.class);
        EventShortDto event = futureEligibleEvent();
        when(users.existsById(RATER_ID)).thenReturn(true);
        when(users.getShortById(RATER_ID)).thenReturn(user);
        when(events.existsById(EVENT_ID)).thenReturn(true);
        when(events.getReviewInfo(EVENT_ID)).thenReturn(event);

        assertThat(validator.findUserOrThrow(RATER_ID)).isSameAs(user);
        assertThat(validator.findEventOrThrow(EVENT_ID)).isSameAs(event);
    }

    @Test
    @DisplayName("Возвращает NotFound для отсутствующих пользователя и события")
    void missingEntities() {
        when(users.existsById(RATER_ID)).thenReturn(false);
        when(events.existsById(EVENT_ID)).thenReturn(false);
        assertThatThrownBy(() -> validator.findUserOrThrow(RATER_ID))
                .isInstanceOf(NotFoundException.class).hasMessage("User with id = 7 not found");
        assertThatThrownBy(() -> validator.findEventOrThrow(EVENT_ID))
                .isInstanceOf(NotFoundException.class).hasMessage("Event with id = 15 not found");
        verify(users, times(0)).getShortById(any());
        verify(events, times(0)).getReviewInfo(any());
    }

    @Test
    @DisplayName("Разрешает оценку события подтвержденному участнику после двух часов")
    void allowsEligibleEventReview() {
        EventShortDto event = eligibleEvent();
        when(participation.isConfirmedParticipant(RATER_ID, EVENT_ID)).thenReturn(true);
        when(eventReviews.existsByParams(RATER_ID, EVENT_ID)).thenReturn(false);

        validator.validateEventReview(RATER_ID, event);

        verify(eventReviews, times(1)).existsByParams(RATER_ID, EVENT_ID);
    }

    @Test
    @DisplayName("Отклоняет оценку события до истечения двух часов")
    void rejectsEarlyReview() {
        EventShortDto event = eligibleEvent();
        event.setEventDate(NOW.minusHours(1));

        assertThatThrownBy(() -> validator.validateEventReview(RATER_ID, event))
                .isInstanceOf(ConflictException.class).hasMessageContaining("2 hours after");
        verifyNoInteractions(participation, eventReviews);
    }

    @Test
    @DisplayName("Отклоняет оценку события от пользователя без подтвержденного участия")
    void rejectsNonParticipant() {
        when(participation.isConfirmedParticipant(RATER_ID, EVENT_ID)).thenReturn(false);

        assertThatThrownBy(() -> validator.validateEventReview(RATER_ID, eligibleEvent()))
                .isInstanceOf(ConflictException.class).hasMessageContaining("not a participant");
        verify(eventReviews, times(0)).existsByParams(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Отклоняет повторную оценку события")
    void rejectsDuplicateEventReview() {
        when(participation.isConfirmedParticipant(RATER_ID, EVENT_ID)).thenReturn(true);
        when(eventReviews.existsByParams(RATER_ID, EVENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> validator.validateEventReview(RATER_ID, eligibleEvent()))
                .isInstanceOf(ConflictException.class).hasMessageContaining("already reviewed");
    }

    @Test
    @DisplayName("Разрешает оценить организатора подтвержденному участнику после задержки")
    void allowsEligibleAuthorReview() {
        when(events.isOrganizer(EVENT_ID, AUTHOR_ID)).thenReturn(true);
        when(participation.isConfirmedParticipant(RATER_ID, EVENT_ID)).thenReturn(true);
        when(userReviews.existsByParams(RATER_ID, EVENT_ID, AUTHOR_ID)).thenReturn(false);

        validator.validateAuthorReview(RATER_ID, AUTHOR_ID, eligibleEvent());

        verify(userReviews, times(1)).existsByParams(RATER_ID, EVENT_ID, AUTHOR_ID);
    }

    @Test
    @DisplayName("Отклоняет оценку пользователя, который не является организатором")
    void rejectsNonOrganizer() {
        when(events.isOrganizer(EVENT_ID, AUTHOR_ID)).thenReturn(false);
        assertThatThrownBy(() -> validator.validateAuthorReview(RATER_ID, AUTHOR_ID, eligibleEvent()))
                .isInstanceOf(ConflictException.class).hasMessageContaining("not the organizer");
        verifyNoInteractions(participation, userReviews);
    }

    @Test
    @DisplayName("Отклоняет оценку организатора до истечения двух часов после события")
    void rejectsEarlyAuthorReview() {
        when(events.isOrganizer(EVENT_ID, AUTHOR_ID)).thenReturn(true);
        EventShortDto event = eligibleEvent();
        event.setEventDate(NOW.minusHours(1));

        assertThatThrownBy(() -> validator.validateAuthorReview(RATER_ID, AUTHOR_ID, event))
                .isInstanceOf(ConflictException.class).hasMessageContaining("2 hours after");
        verifyNoInteractions(participation, userReviews);
    }

    @Test
    @DisplayName("Отклоняет оценку организатора от пользователя без подтвержденного участия")
    void rejectsAuthorReviewFromNonParticipant() {
        when(events.isOrganizer(EVENT_ID, AUTHOR_ID)).thenReturn(true);
        when(participation.isConfirmedParticipant(RATER_ID, EVENT_ID)).thenReturn(false);

        assertThatThrownBy(() -> validator.validateAuthorReview(RATER_ID, AUTHOR_ID, eligibleEvent()))
                .isInstanceOf(ConflictException.class).hasMessageContaining("not a participant");
        verify(userReviews, times(0)).existsByParams(anyLong(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("Отклоняет повторную оценку организатора за то же событие")
    void rejectsDuplicateAuthorReview() {
        when(events.isOrganizer(EVENT_ID, AUTHOR_ID)).thenReturn(true);
        when(participation.isConfirmedParticipant(RATER_ID, EVENT_ID)).thenReturn(true);
        when(userReviews.existsByParams(RATER_ID, EVENT_ID, AUTHOR_ID)).thenReturn(true);
        assertThatThrownBy(() -> validator.validateAuthorReview(RATER_ID, AUTHOR_ID, eligibleEvent()))
                .isInstanceOf(ConflictException.class).hasMessageContaining("already reviewed");
    }

    private EventShortDto eligibleEvent() {
        EventShortDto event = random.nextObject(EventShortDto.class);
        event.setId(EVENT_ID);
        event.setEventDate(NOW.minusHours(3));
        return event;
    }

    private EventShortDto futureEligibleEvent() {
        return eligibleEvent();
    }
}

package ru.practicum.core.review.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.practicum.core.common.exception.ConflictException;
import ru.practicum.core.common.time.TimeProvider;
import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.core.review.dto.NewEventReviewDto;
import ru.practicum.core.review.interservice.EventInterservice;
import ru.practicum.core.review.interservice.ParticipationInterservice;
import ru.practicum.core.review.interservice.UserInterservice;
import ru.practicum.core.review.mapper.ReviewMapperImpl;
import ru.practicum.core.review.model.EventReview;
import ru.practicum.core.review.repository.EventReviewRepository;
import ru.practicum.core.review.validation.ReviewValidatorImpl;
import ru.practicum.core.user.dto.UserShortDto;
import ru.practicum.ewm.stats.model.action.ActionType;
import ru.practicum.ewm.stats.model.action.UserAction;
import ru.practicum.stats.client.grpc.CollectorGrpcClient;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DataJpaTest(showSql = false, properties = {
        "spring.cloud.config.enabled=false", "spring.jpa.hibernate.ddl-auto=validate", "spring.sql.init.mode=always"
})
@ContextConfiguration(classes = ReviewLikeIntegrationTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ReviewLikeIntegrationTest {
    private static final long USER_ID = 67L;
    private static final long EVENT_ID = 42L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 8, 15, 0);

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = EventReview.class)
    @EnableJpaRepositories(basePackageClasses = EventReviewRepository.class)
    @Import({ReviewServiceImpl.class, ReviewLikeListener.class, ReviewMapperImpl.class, ReviewValidatorImpl.class})
    static class Config {
    }

    @Autowired
    private ReviewService reviews;
    @Autowired
    private EventReviewRepository repository;
    @Autowired
    private TransactionTemplate transaction;
    @MockBean
    private CollectorGrpcClient collector;
    @MockBean
    private EventInterservice events;
    @MockBean
    private UserInterservice users;
    @MockBean
    private ParticipationInterservice participation;
    @MockBean
    private TimeProvider time;

    @BeforeEach
    void prepareReview() {
        repository.deleteAll();
        when(users.existsById(USER_ID)).thenReturn(true);
        when(users.getShortById(USER_ID)).thenReturn(UserShortDto.builder().id(USER_ID).name("User").build());
        when(events.existsById(EVENT_ID)).thenReturn(true);
        when(events.getReviewInfo(EVENT_ID)).thenReturn(
                EventShortDto.builder().id(EVENT_ID).eventDate(NOW.minusHours(3)).build());
        when(time.now()).thenReturn(NOW);
    }

    @AfterEach
    void verifyLookupInteractions() {
        verify(users, times(1)).existsById(USER_ID);
        verify(users, times(1)).getShortById(USER_ID);
        verify(events, times(1)).existsById(EVENT_ID);
        verify(events, times(1)).getReviewInfo(EVENT_ID);
        verify(time, times(1)).now();
        verify(participation, times(1)).isConfirmedParticipant(USER_ID, EVENT_ID);
        verifyNoMoreInteractions(users, events, time, participation, collector);
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 5})
    @DisplayName("High event scores send one LIKE after commit")
    void sendsLikeAfterCommit(int score) {
        when(participation.isConfirmedParticipant(USER_ID, EVENT_ID)).thenReturn(true);

        transaction.executeWithoutResult(status -> {
            assertThat(reviews.createEventReview(USER_ID, EVENT_ID, new NewEventReviewDto(score, null)).getScore())
                    .isEqualTo(score);
            verifyNoInteractions(collector);
        });

        assertThat(repository.findAll()).extracting(EventReview::getScore).containsExactly(score);
        verify(collector, times(1)).collectUserAction(argThat((UserAction action) ->
                action.userId() == USER_ID && action.eventId() == EVENT_ID
                        && action.actionType() == ActionType.LIKE && action.timestamp() != null));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    @DisplayName("Low event scores are saved without a LIKE")
    void savesLowerScoreWithoutLike(int score) {
        when(participation.isConfirmedParticipant(USER_ID, EVENT_ID)).thenReturn(true);

        assertThat(reviews.createEventReview(USER_ID, EVENT_ID, new NewEventReviewDto(score, null)).getScore())
                .isEqualTo(score);

        assertThat(repository.findAll()).extracting(EventReview::getScore).containsExactly(score);
        verifyNoInteractions(collector);
    }

    @Test
    @DisplayName("A rolled-back high score never sends a LIKE")
    void skipsLikeOnRollback() {
        when(participation.isConfirmedParticipant(USER_ID, EVENT_ID)).thenReturn(true);

        transaction.executeWithoutResult(status -> {
            reviews.createEventReview(USER_ID, EVENT_ID, new NewEventReviewDto(5, null));
            status.setRollbackOnly();
        });

        assertThat(repository.findAll()).isEmpty();
        verifyNoInteractions(collector);
    }

    @Test
    @DisplayName("A user without confirmed participation cannot rate or like the event")
    void rejectsNonParticipant() {
        when(participation.isConfirmedParticipant(USER_ID, EVENT_ID)).thenReturn(false);

        assertThatThrownBy(() -> reviews.createEventReview(USER_ID, EVENT_ID, new NewEventReviewDto(5, null)))
                .isInstanceOf(ConflictException.class);

        assertThat(repository.findAll()).isEmpty();
        verifyNoInteractions(collector);
    }
}

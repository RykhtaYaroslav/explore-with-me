package ru.practicum.core.participation.validation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.core.common.enums.RequestStatus;
import ru.practicum.core.common.exception.ConflictException;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.event.dto.response.EventDtoForParticipationService;
import ru.practicum.core.event.model.EventState;
import ru.practicum.core.participation.dto.EventRequestStatusUpdateRequest;
import ru.practicum.core.participation.interservice.EventInterservice;
import ru.practicum.core.participation.interservice.UserInterservice;
import ru.practicum.core.participation.model.ParticipationRequest;
import ru.practicum.core.participation.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestValidatorImplTest {
    private static final long USER_ID = 7L;
    private static final long EVENT_ID = 15L;
    @Mock private UserInterservice users;
    @Mock private EventInterservice events;
    @Mock private RequestRepository requests;
    private RequestValidatorImpl validator;

    @BeforeEach
    void setUp() {
        validator = new RequestValidatorImpl(users, events, requests);
    }

    @AfterEach
    void verifyNoMoreMockInteractions() {
        verifyNoMoreInteractions(users, events, requests);
    }

    @Test
    @DisplayName("Назначает CONFIRMED событию без лимита и без модерации")
    void returnsConfirmedForUnmoderatedUnlimitedEvent() {
        when(events.findByIdOrThrow(EVENT_ID)).thenReturn(event(42L, 0, false, 0L));
        when(requests.existsByEventIdAndRequesterId(EVENT_ID, USER_ID)).thenReturn(false);
        when(requests.countByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED)).thenReturn(0L);

        assertThat(validator.validateCreateRequestAndGetStatus(USER_ID, EVENT_ID)).isEqualTo(RequestStatus.CONFIRMED);
        verify(users, times(1)).existsByIdOrThrow(USER_ID);
        verify(events, times(1)).findByIdOrThrow(EVENT_ID);
        verify(requests, times(1)).existsByEventIdAndRequesterId(EVENT_ID, USER_ID);
        verify(requests, times(1)).countByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Запрещает инициатору подавать заявку на собственное событие")
    void rejectsInitiatorAsRequester() {
        when(events.findByIdOrThrow(EVENT_ID)).thenReturn(event(USER_ID, 10, true, 0L));
        when(requests.existsByEventIdAndRequesterId(EVENT_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> validator.validateCreateRequestAndGetStatus(USER_ID, EVENT_ID))
                .isInstanceOf(ConflictException.class).hasMessageContaining("initiator");
        verify(users, times(1)).existsByIdOrThrow(USER_ID);
        verify(events, times(1)).findByIdOrThrow(EVENT_ID);
        verify(requests, times(1)).existsByEventIdAndRequesterId(EVENT_ID, USER_ID);
        verify(requests, times(0)).countByEventIdAndStatus(anyLong(), any());
    }

    @Test
    @DisplayName("Запрещает заявку при достигнутом лимите участников")
    void rejectsWhenParticipantLimitReached() {
        when(events.findByIdOrThrow(EVENT_ID)).thenReturn(event(100L, 2, true, 2L));
        when(requests.existsByEventIdAndRequesterId(EVENT_ID, USER_ID)).thenReturn(false);
        when(requests.countByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED)).thenReturn(2L);

        assertThatThrownBy(() -> validator.validateCreateRequestAndGetStatus(USER_ID, EVENT_ID))
                .isInstanceOf(ConflictException.class).hasMessageContaining("no available spots");
        verify(users, times(1)).existsByIdOrThrow(USER_ID);
        verify(events, times(1)).findByIdOrThrow(EVENT_ID);
        verify(requests, times(1)).existsByEventIdAndRequesterId(EVENT_ID, USER_ID);
        verify(requests, times(1)).countByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Отклоняет подтверждение, если лимит равен нулю или модерация отключена")
    void rejectsStatusChangeWhenModerationIsNotRequired() {
        when(events.findByIdForOrganizerOrThrow(EVENT_ID, USER_ID)).thenReturn(event(100L, 0, true, 0L));
        EventRequestStatusUpdateRequest update = new EventRequestStatusUpdateRequest(List.of(1L), RequestStatus.CONFIRMED);

        assertThatThrownBy(() -> validator.validateChangeRequestsStatus(USER_ID, EVENT_ID, update))
                .isInstanceOf(ConflictException.class).hasMessageContaining("Confirmation is not required");
        verify(events, times(1)).findByIdForOrganizerOrThrow(EVENT_ID, USER_ID);
        verifyNoInteractions(requests);
    }

    @Test
    @DisplayName("Отклоняет подтверждение, если модерация отключена")
    void rejectsStatusChangeWhenModerationDisabled() {
        when(events.findByIdForOrganizerOrThrow(EVENT_ID, USER_ID)).thenReturn(event(100L, 5, false, 0L));
        EventRequestStatusUpdateRequest update = new EventRequestStatusUpdateRequest(List.of(1L), RequestStatus.CONFIRMED);

        assertThatThrownBy(() -> validator.validateChangeRequestsStatus(USER_ID, EVENT_ID, update))
                .isInstanceOf(ConflictException.class);
        verify(events, times(1)).findByIdForOrganizerOrThrow(EVENT_ID, USER_ID);
        verifyNoInteractions(requests);
    }

    @Test
    @DisplayName("Отклоняет обновление статусов, если лимит уже достигнут")
    void rejectsStatusChangeWhenLimitAlreadyReached() {
        when(events.findByIdForOrganizerOrThrow(EVENT_ID, USER_ID)).thenReturn(event(100L, 2, true, 2L));
        when(requests.countByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED)).thenReturn(2L);
        EventRequestStatusUpdateRequest update = new EventRequestStatusUpdateRequest(List.of(1L), RequestStatus.CONFIRMED);

        assertThatThrownBy(() -> validator.validateChangeRequestsStatus(USER_ID, EVENT_ID, update))
                .isInstanceOf(ConflictException.class).hasMessageContaining("participant limit has been reached");
        verify(events, times(1)).findByIdForOrganizerOrThrow(EVENT_ID, USER_ID);
        verify(requests, times(1)).countByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Возвращает NotFound, если не найдены все запрошенные заявки")
    void reportsMissingRequestIds() {
        when(events.findByIdForOrganizerOrThrow(EVENT_ID, USER_ID)).thenReturn(event(100L, 5, true, 0L));
        when(requests.countByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED)).thenReturn(0L);
        when(requests.findAllById(Set.of(1L, 2L))).thenReturn(List.of(request(1L, EVENT_ID, RequestStatus.PENDING)));
        EventRequestStatusUpdateRequest update = new EventRequestStatusUpdateRequest(
                new java.util.ArrayList<>(List.of(1L, 2L)), RequestStatus.CONFIRMED);

        assertThatThrownBy(() -> validator.validateChangeRequestsStatus(USER_ID, EVENT_ID, update))
                .isInstanceOf(NotFoundException.class).hasMessageContaining("Some requests were not found");
        verify(events, times(1)).findByIdForOrganizerOrThrow(EVENT_ID, USER_ID);
        verify(requests, times(1)).countByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED);
        verify(requests, times(1)).findAllById(Set.of(1L, 2L));
    }

    @Test
    @DisplayName("Отклоняет заявку, которая уже не находится в статусе PENDING")
    void rejectsAlreadyProcessedRequests() {
        when(events.findByIdForOrganizerOrThrow(EVENT_ID, USER_ID)).thenReturn(event(100L, 5, true, 0L));
        when(requests.countByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED)).thenReturn(0L);
        when(requests.findAllById(Set.of(1L))).thenReturn(List.of(request(1L, EVENT_ID, RequestStatus.REJECTED)));
        EventRequestStatusUpdateRequest update = new EventRequestStatusUpdateRequest(
                new java.util.ArrayList<>(List.of(1L)), RequestStatus.CONFIRMED);

        assertThatThrownBy(() -> validator.validateChangeRequestsStatus(USER_ID, EVENT_ID, update))
                .isInstanceOf(ConflictException.class).hasMessageContaining("All requests must be pending");
        verify(events, times(1)).findByIdForOrganizerOrThrow(EVENT_ID, USER_ID);
        verify(requests, times(1)).countByEventIdAndStatus(EVENT_ID, RequestStatus.CONFIRMED);
        verify(requests, times(1)).findAllById(Set.of(1L));
    }

    @Test
    @DisplayName("Не проверяет событие и заявки, если пользователь не найден при чтении заявок события")
    void rejectsEventRequestLookupWhenUserIsMissing() {
        doThrow(new NotFoundException("missing user")).when(users).existsByIdOrThrow(USER_ID);

        assertThatThrownBy(() -> validator.validateEventRequestsAccess(USER_ID, EVENT_ID))
                .isInstanceOf(NotFoundException.class).hasMessage("missing user");

        verify(users).existsByIdOrThrow(USER_ID);
        verifyNoInteractions(events, requests);
    }

    @Test
    @DisplayName("Не читает заявки, если событие не принадлежит инициатору")
    void rejectsEventRequestLookupWhenEventIsMissingForInitiator() {
        doThrow(new NotFoundException("missing event")).when(events).ensureOrganizerOrThrow(EVENT_ID, USER_ID);

        assertThatThrownBy(() -> validator.validateEventRequestsAccess(USER_ID, EVENT_ID))
                .isInstanceOf(NotFoundException.class).hasMessage("missing event");

        verify(users).existsByIdOrThrow(USER_ID);
        verify(events).ensureOrganizerOrThrow(EVENT_ID, USER_ID);
        verifyNoInteractions(requests);
    }

    @Test
    @DisplayName("Отклоняет изменение статуса при нулевом лимите")
    void rejectsStatusChangeWhenParticipantLimitIsZero() {
        when(events.findByIdForOrganizerOrThrow(EVENT_ID, USER_ID)).thenReturn(event(100L, 0, true, 0L));
        EventRequestStatusUpdateRequest update = new EventRequestStatusUpdateRequest(List.of(1L), RequestStatus.CONFIRMED);

        assertThatThrownBy(() -> validator.validateChangeRequestsStatus(USER_ID, EVENT_ID, update))
                .isInstanceOf(ConflictException.class).hasMessageContaining("Confirmation is not required");

        verify(events).findByIdForOrganizerOrThrow(EVENT_ID, USER_ID);
        verifyNoInteractions(requests);
    }

    @Test
    @DisplayName("Отклоняет запросы события от пользователя, который не является организатором")
    void validatesOrganizerBeforeReadingRequests() {
        doThrow(new NotFoundException("not organizer")).when(events).ensureOrganizerOrThrow(EVENT_ID, USER_ID);
        assertThatThrownBy(() -> validator.validateEventRequestsAccess(USER_ID, EVENT_ID))
                .isInstanceOf(NotFoundException.class).hasMessage("not organizer");
        verify(users, times(1)).existsByIdOrThrow(USER_ID);
        verify(events, times(1)).ensureOrganizerOrThrow(EVENT_ID, USER_ID);
        verifyNoInteractions(requests);
    }

    private EventDtoForParticipationService event(Long initiatorId, int limit, boolean moderation, long confirmed) {
        return new EventDtoForParticipationService(EVENT_ID, EventState.PUBLISHED,
                LocalDateTime.now().plusDays(2), initiatorId, limit, moderation, confirmed);
    }

    private ParticipationRequest request(Long id, Long eventId, RequestStatus status) {
        return ParticipationRequest.builder().id(id).eventId(eventId).requesterId(USER_ID).status(status).build();
    }
}

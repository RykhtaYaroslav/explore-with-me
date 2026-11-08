package ru.practicum.core.participation.service;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import ru.practicum.core.common.enums.RequestStatus;
import ru.practicum.core.common.exception.ConflictException;
import ru.practicum.core.participation.client.EventFeignClient;
import ru.practicum.core.participation.dto.EventRequestStatusUpdateRequest;
import ru.practicum.core.participation.dto.ParticipationRequestDto;
import ru.practicum.core.participation.dto.internal.RequestStatusUpdateContext;
import ru.practicum.core.participation.dto.mapper.RequestMapper;
import ru.practicum.core.participation.model.ParticipationRequest;
import ru.practicum.core.participation.repository.RequestRepository;
import ru.practicum.core.participation.util.TestDataUtils;
import ru.practicum.core.participation.validation.RequestValidator;
import ru.practicum.ewm.stats.model.action.ActionType;
import ru.practicum.ewm.stats.model.action.UserAction;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {
    @Mock
    private RequestRepository repository;
    @Mock
    private RequestValidator validator;
    @Mock
    private RequestMapper mapper;
    @Mock
    private EventFeignClient eventClient;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @InjectMocks
    private RequestServiceImpl service;
    private EasyRandom random;

    @BeforeEach
    void setUp() {
        random = TestDataUtils.createEasyRandomizer();
    }

    @AfterEach
    void verifyNoMoreMockInteractions() {
        verifyNoMoreInteractions(repository, validator, mapper, eventClient, eventPublisher);
    }

    @Test
    @DisplayName("Создает подтвержденную заявку без модерации и синхронизирует счетчик события")
    void createConfirmedRequest() {
        long userId = 34L;
        long eventId = 56L;
        ParticipationRequest saved = random.nextObject(ParticipationRequest.class);
        saved.setId(78L);
        saved.setRequesterId(userId);
        saved.setEventId(eventId);
        saved.setStatus(RequestStatus.CONFIRMED);
        ParticipationRequestDto dto = random.nextObject(ParticipationRequestDto.class);
        when(validator.validateCreateRequestAndGetStatus(userId, eventId)).thenReturn(RequestStatus.CONFIRMED);
        when(repository.save(any(ParticipationRequest.class))).thenReturn(saved);
        when(mapper.toDtoOut(saved)).thenReturn(dto);

        ParticipationRequestDto result = service.createRequest(userId, eventId);

        assertThat(result).isSameAs(dto);
        verify(validator, times(1)).validateCreateRequestAndGetStatus(userId, eventId);
        verify(repository, times(1)).save(argThat(request -> request.getRequesterId().equals(userId)
                && request.getEventId().equals(eventId) && request.getStatus() == RequestStatus.CONFIRMED));
        verify(mapper, times(1)).toDtoOut(saved);
        verify(eventClient, times(1)).adjustConfirmedRequestCount(eventId, 1);
        verify(eventPublisher, times(1)).publishEvent(argThat((UserAction action) ->
                action.userId() == userId && action.eventId() == eventId && action.actionType() == ActionType.REGISTER));
    }

    @Test
    @DisplayName("Не создает заявку, если инициатор пытается записаться на собственное событие")
    void rejectsInitiatorRequest() {
        long userId = 34L;
        long eventId = 56L;
        when(validator.validateCreateRequestAndGetStatus(userId, eventId))
                .thenThrow(new ConflictException("initiator"));

        assertThatThrownBy(() -> service.createRequest(userId, eventId))
                .isInstanceOf(ConflictException.class).hasMessage("initiator");
        verify(validator, times(1)).validateCreateRequestAndGetStatus(userId, eventId);
        verifyNoInteractions(repository, eventClient, mapper, eventPublisher);
    }

    @Test
    @DisplayName("Не создает заявку, если лимит участников уже достигнут")
    void rejectsRequestWhenLimitReached() {
        when(validator.validateCreateRequestAndGetStatus(34L, 56L))
                .thenThrow(new ConflictException("limit"));

        assertThatThrownBy(() -> service.createRequest(34L, 56L))
                .isInstanceOf(ConflictException.class).hasMessage("limit");
        verify(validator, times(1)).validateCreateRequestAndGetStatus(34L, 56L);
        verifyNoInteractions(repository, eventClient, mapper, eventPublisher);
    }

    @Test
    @DisplayName("Отменяет собственную заявку и возвращает статус CANCELED")
    void cancelsOwnRequest() {
        ParticipationRequest request = random.nextObject(ParticipationRequest.class);
        request.setId(78L);
        request.setRequesterId(34L);
        request.setEventId(56L);
        request.setStatus(RequestStatus.CONFIRMED);
        when(repository.findById(78L)).thenReturn(Optional.of(request));
        ParticipationRequestDto dto = random.nextObject(ParticipationRequestDto.class);
        when(mapper.toDtoOut(request)).thenReturn(dto);

        ParticipationRequestDto result = service.cancelRequest(34L, 78L);

        assertThat(result).isSameAs(dto);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.CANCELED);
        verify(repository, times(1)).findById(78L);
        verify(validator, times(1)).validateCancelRequest(34L, request);
        verify(repository, times(1)).save(request);
        verify(eventClient, times(1)).adjustConfirmedRequestCount(56L, -1);
        verify(mapper, times(1)).toDtoOut(request);
    }

    @Test
    @DisplayName("Возвращает заявки события для прошедшего проверку инициатора")
    void getsRequestsForOrganizer() {
        ParticipationRequest request = random.nextObject(ParticipationRequest.class);
        when(repository.findAllByEventId(56L)).thenReturn(List.of(request));
        ParticipationRequestDto dto = random.nextObject(ParticipationRequestDto.class);
        when(mapper.toDtoOut(request)).thenReturn(dto);

        assertThat(service.getEventRequestsByInitiator(34L, 56L)).containsExactly(dto);
        verify(validator, times(1)).validateEventRequestsAccess(34L, 56L);
        verify(repository, times(1)).findAllByEventId(56L);
        verify(mapper, times(1)).toDtoOut(request);
    }

    @Test
    @DisplayName("Возвращает пустой список заявок события без преобразования DTO")
    void getsEmptyOrganizerRequests() {
        when(repository.findAllByEventId(56L)).thenReturn(List.of());

        assertThat(service.getEventRequestsByInitiator(34L, 56L)).isEmpty();
        verify(validator, times(1)).validateEventRequestsAccess(34L, 56L);
        verify(repository, times(1)).findAllByEventId(56L);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("Подтверждает выбранную заявку и обновляет счетчик события")
    void confirmsSelectedRequests() {
        ParticipationRequest request = random.nextObject(ParticipationRequest.class);
        request.setId(78L);
        request.setEventId(56L);
        request.setStatus(RequestStatus.PENDING);
        EventRequestStatusUpdateRequest update = new EventRequestStatusUpdateRequest(List.of(78L), RequestStatus.CONFIRMED);
        when(validator.validateChangeRequestsStatus(34L, 56L, update))
                .thenReturn(new RequestStatusUpdateContext(RequestStatus.CONFIRMED, List.of(request), 0, 3));
        when(mapper.toDtoOut(request)).thenAnswer(invocation -> {
            ParticipationRequestDto dto = random.nextObject(ParticipationRequestDto.class);
            dto.setId(request.getId());
            dto.setStatus(request.getStatus());
            return dto;
        });

        var result = service.changeRequestsStatus(34L, 56L, update);

        assertThat(request.getStatus()).isEqualTo(RequestStatus.CONFIRMED);
        assertThat(result.getConfirmedRequests()).hasSize(1);
        assertThat(result.getRejectedRequests()).isEmpty();
        verify(validator, times(1)).validateChangeRequestsStatus(34L, 56L, update);
        verify(repository, times(1)).saveAll(List.of(request));
        verify(mapper, times(1)).toDtoOut(request);
        verify(eventClient, times(1)).adjustConfirmedRequestCount(56L, 1);
    }

    @Test
    @DisplayName("Отклоняет заявки, выбранные организатором")
    void rejectsSelectedRequests() {
        ParticipationRequest request = random.nextObject(ParticipationRequest.class);
        request.setStatus(RequestStatus.PENDING);
        EventRequestStatusUpdateRequest update = new EventRequestStatusUpdateRequest(List.of(78L), RequestStatus.REJECTED);
        when(validator.validateChangeRequestsStatus(34L, 56L, update))
                .thenReturn(new RequestStatusUpdateContext(RequestStatus.REJECTED, List.of(request), 0, 3));
        when(mapper.toDtoOut(request)).thenReturn(random.nextObject(ParticipationRequestDto.class));

        var result = service.changeRequestsStatus(34L, 56L, update);

        assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(result.getRejectedRequests()).hasSize(1);
        assertThat(result.getConfirmedRequests()).isEmpty();
        verify(validator, times(1)).validateChangeRequestsStatus(34L, 56L, update);
        verify(repository, times(1)).saveAll(List.of(request));
        verify(mapper, times(1)).toDtoOut(request);
        verifyNoInteractions(eventClient);
    }

    @Test
    @DisplayName("При заполнении лимита отклоняет остальные ожидающие заявки события")
    void rejectsOtherPendingRequestsWhenCapacityIsFull() {
        ParticipationRequest selected = request(78L);
        ParticipationRequest other = request(79L);
        EventRequestStatusUpdateRequest update = new EventRequestStatusUpdateRequest(List.of(78L), RequestStatus.CONFIRMED);
        when(validator.validateChangeRequestsStatus(34L, 56L, update))
                .thenReturn(new RequestStatusUpdateContext(RequestStatus.CONFIRMED, List.of(selected), 1, 2));
        when(repository.findAllByEventIdAndStatusAndIdNotIn(56L, RequestStatus.PENDING, java.util.Set.of(78L)))
                .thenReturn(List.of(other));
        when(mapper.toDtoOut(selected)).thenReturn(random.nextObject(ParticipationRequestDto.class));
        when(mapper.toDtoOut(other)).thenReturn(random.nextObject(ParticipationRequestDto.class));

        var result = service.changeRequestsStatus(34L, 56L, update);

        assertThat(selected.getStatus()).isEqualTo(RequestStatus.CONFIRMED);
        assertThat(other.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(result.getConfirmedRequests()).hasSize(1);
        assertThat(result.getRejectedRequests()).hasSize(1);
        verify(validator, times(1)).validateChangeRequestsStatus(34L, 56L, update);
        verify(repository, times(1)).findAllByEventIdAndStatusAndIdNotIn(
                56L, RequestStatus.PENDING, java.util.Set.of(78L));
        verify(repository, times(1)).saveAll(List.of(selected));
        verify(repository, times(1)).saveAll(List.of(other));
        verify(mapper, times(1)).toDtoOut(selected);
        verify(mapper, times(1)).toDtoOut(other);
        verify(eventClient, times(1)).adjustConfirmedRequestCount(56L, 1);
    }

    @Test
    @DisplayName("При превышении остатка мест подтверждает только заявки, которые помещаются")
    void confirmsOnlyRequestsWithinRemainingCapacity() {
        ParticipationRequest first = request(78L);
        ParticipationRequest second = request(79L);
        EventRequestStatusUpdateRequest update = new EventRequestStatusUpdateRequest(List.of(78L, 79L), RequestStatus.CONFIRMED);
        when(validator.validateChangeRequestsStatus(34L, 56L, update))
                .thenReturn(new RequestStatusUpdateContext(RequestStatus.CONFIRMED, List.of(first, second), 1, 2));
        when(mapper.toDtoOut(first)).thenReturn(random.nextObject(ParticipationRequestDto.class));
        when(mapper.toDtoOut(second)).thenReturn(random.nextObject(ParticipationRequestDto.class));

        var result = service.changeRequestsStatus(34L, 56L, update);

        assertThat(first.getStatus()).isEqualTo(RequestStatus.CONFIRMED);
        assertThat(second.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(result.getConfirmedRequests()).hasSize(1);
        assertThat(result.getRejectedRequests()).hasSize(1);
        verify(validator, times(1)).validateChangeRequestsStatus(34L, 56L, update);
        verify(repository, times(1)).findAllByEventIdAndStatusAndIdNotIn(
                56L, RequestStatus.PENDING, java.util.Set.of(78L, 79L));
        verify(repository, times(1)).saveAll(List.of(first, second));
        verify(repository, times(1)).saveAll(List.of(second));
        verify(mapper, times(1)).toDtoOut(first);
        verify(mapper, times(1)).toDtoOut(second);
        verify(eventClient, times(1)).adjustConfirmedRequestCount(56L, 1);
    }

    private ParticipationRequest request(Long id) {
        ParticipationRequest request = random.nextObject(ParticipationRequest.class);
        request.setId(id);
        request.setEventId(56L);
        request.setStatus(RequestStatus.PENDING);
        return request;
    }
}

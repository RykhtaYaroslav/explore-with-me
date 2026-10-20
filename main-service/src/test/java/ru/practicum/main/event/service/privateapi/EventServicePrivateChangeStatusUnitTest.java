package ru.practicum.main.event.service.privateapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.main.event.dto.EventRequestStatusUpdateRequest;
import ru.practicum.main.event.dto.EventRequestStatusUpdateResult;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.event.service.EventStatsCollector;
import ru.practicum.main.exception.EventUpdateException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.request.dto.ParticipationRequestDto;
import ru.practicum.main.request.dto.mapper.RequestMapper;
import ru.practicum.main.request.model.ParticipationRequest;
import ru.practicum.main.request.model.RequestStatus;
import ru.practicum.main.request.repository.RequestRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServicePrivateChangeStatusUnitTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private RequestMapper requestMapper;

    @Mock
    private EventStatsCollector eventStatsCollector;

    @InjectMocks
    private EventServicePrivateImpl eventService;

    private final Long userId = 1L;
    private final Long eventId = 10L;

    @Test
    @DisplayName("Успешное подтверждение заявок, когда лимит еще не исчерпан")
    void changeRequestsStatus_whenConfirmingAndLimitNotReached_shouldConfirmRequests() {
        Event event = Event.builder()
                .id(eventId)
                .participantLimit(10)
                .requestModeration(true)
                .build();

        EventRequestStatusUpdateRequest requestDto = EventRequestStatusUpdateRequest.builder()
                .requestsIds(List.of(100L, 101L))
                .status(RequestStatus.CONFIRMED)
                .build();

        ParticipationRequest req1 = ParticipationRequest.builder().id(100L).status(RequestStatus.PENDING).build();
        ParticipationRequest req2 = ParticipationRequest.builder().id(101L).status(RequestStatus.PENDING).build();

        ParticipationRequestDto dto1 = ParticipationRequestDto.builder().id(100L).status(RequestStatus.CONFIRMED).build();
        ParticipationRequestDto dto2 = ParticipationRequestDto.builder().id(101L).status(RequestStatus.CONFIRMED).build();

        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.of(event));
        when(eventStatsCollector.getConReqByEventMap(List.of(event))).thenReturn(Map.of(eventId, 2L));
        when(requestRepository.findAllById(Set.of(100L, 101L))).thenReturn(List.of(req1, req2));
        when(requestMapper.toDtoOut(req1)).thenReturn(dto1);
        when(requestMapper.toDtoOut(req2)).thenReturn(dto2);

        EventRequestStatusUpdateResult result = eventService.changeRequestsStatus(userId, eventId, requestDto);

        assertNotNull(result);
        assertEquals(2, result.getConfirmedRequests().size());
        assertTrue(result.getRejectedRequests().isEmpty());
        assertEquals(RequestStatus.CONFIRMED, req1.getStatus());
        assertEquals(RequestStatus.CONFIRMED, req2.getStatus());
        verify(requestRepository).saveAll(List.of(req1, req2));
    }

    @Test
    @DisplayName("Автоматическое отклонение всех остальных PENDING заявок при точном достижении лимита")
    void changeRequestsStatus_whenConfirmingReachesExactLimit_shouldRejectRemainingPendingRequests() {
        Event event = Event.builder()
                .id(eventId)
                .participantLimit(3)
                .requestModeration(true)
                .build();

        EventRequestStatusUpdateRequest requestDto = EventRequestStatusUpdateRequest.builder()
                .requestsIds(List.of(100L))
                .status(RequestStatus.CONFIRMED)
                .build();

        ParticipationRequest targetReq = ParticipationRequest.builder().id(100L).status(RequestStatus.PENDING).build();
        ParticipationRequest otherPendingReq = ParticipationRequest.builder().id(102L).status(RequestStatus.PENDING).build();

        ParticipationRequestDto confirmedDto = ParticipationRequestDto.builder().id(100L).status(RequestStatus.CONFIRMED).build();
        ParticipationRequestDto rejectedDto = ParticipationRequestDto.builder().id(102L).status(RequestStatus.REJECTED).build();

        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.of(event));
        when(eventStatsCollector.getConReqByEventMap(List.of(event))).thenReturn(Map.of(eventId, 2L));
        when(requestRepository.findAllById(Set.of(100L))).thenReturn(List.of(targetReq));
        when(requestRepository.findAllByEventId(eventId)).thenReturn(List.of(targetReq, otherPendingReq));
        when(requestMapper.toDtoOut(targetReq)).thenReturn(confirmedDto);
        when(requestMapper.toDtoOut(otherPendingReq)).thenReturn(rejectedDto);

        EventRequestStatusUpdateResult result = eventService.changeRequestsStatus(userId, eventId, requestDto);

        assertEquals(1, result.getConfirmedRequests().size());
        assertEquals(1, result.getRejectedRequests().size());
        assertEquals(RequestStatus.CONFIRMED, targetReq.getStatus());
        assertEquals(RequestStatus.REJECTED, otherPendingReq.getStatus());
    }

    @Test
    @DisplayName("Частичное подтверждение и отклонение входящих заявок при превышении лимита")
    void changeRequestsStatus_whenConfirmingExceedsLimit_shouldConfirmPartialAndRejectRest() {
        Event event = Event.builder()
                .id(eventId)
                .participantLimit(2)
                .requestModeration(true)
                .build();

        EventRequestStatusUpdateRequest requestDto = EventRequestStatusUpdateRequest.builder()
                .requestsIds(List.of(100L, 101L))
                .status(RequestStatus.CONFIRMED)
                .build();

        ParticipationRequest req1 = ParticipationRequest.builder().id(100L).status(RequestStatus.PENDING).build();
        ParticipationRequest req2 = ParticipationRequest.builder().id(101L).status(RequestStatus.PENDING).build();

        ParticipationRequestDto dto1 = ParticipationRequestDto.builder().id(100L).status(RequestStatus.CONFIRMED).build();
        ParticipationRequestDto dto2 = ParticipationRequestDto.builder().id(101L).status(RequestStatus.REJECTED).build();

        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.of(event));
        when(eventStatsCollector.getConReqByEventMap(List.of(event))).thenReturn(Map.of(eventId, 1L));
        when(requestRepository.findAllById(Set.of(100L, 101L))).thenReturn(List.of(req1, req2));
        when(requestRepository.findAllByEventId(eventId)).thenReturn(List.of(req1, req2));
        when(requestMapper.toDtoOut(req1)).thenReturn(dto1);
        when(requestMapper.toDtoOut(req2)).thenReturn(dto2);

        EventRequestStatusUpdateResult result = eventService.changeRequestsStatus(userId, eventId, requestDto);

        assertEquals(1, result.getConfirmedRequests().size());
        assertEquals(1, result.getRejectedRequests().size());
        assertEquals(RequestStatus.CONFIRMED, req1.getStatus());
        assertEquals(RequestStatus.REJECTED, req2.getStatus());
    }

    @Test
    @DisplayName("Успешное явное отклонение заявок")
    void changeRequestsStatus_whenRejectingRequests_shouldRejectSpecifiedRequests() {
        Event event = Event.builder()
                .id(eventId)
                .participantLimit(10)
                .requestModeration(true)
                .build();

        EventRequestStatusUpdateRequest requestDto = EventRequestStatusUpdateRequest.builder()
                .requestsIds(List.of(100L))
                .status(RequestStatus.REJECTED)
                .build();

        ParticipationRequest req = ParticipationRequest.builder().id(100L).status(RequestStatus.PENDING).build();
        ParticipationRequestDto rejectedDto = ParticipationRequestDto.builder().id(100L).status(RequestStatus.REJECTED).build();

        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.of(event));
        when(eventStatsCollector.getConReqByEventMap(List.of(event))).thenReturn(Collections.emptyMap());
        when(requestRepository.findAllById(Set.of(100L))).thenReturn(List.of(req));
        when(requestMapper.toDtoOut(req)).thenReturn(rejectedDto);

        EventRequestStatusUpdateResult result = eventService.changeRequestsStatus(userId, eventId, requestDto);

        assertTrue(result.getConfirmedRequests().isEmpty());
        assertEquals(1, result.getRejectedRequests().size());
        assertEquals(RequestStatus.REJECTED, req.getStatus());
        verify(requestRepository).saveAll(List.of(req));
    }

    @Test
    @DisplayName("Выброс исключения, если лимит участников равен 0")
    void changeRequestsStatus_whenLimitIsZero_shouldThrowException() {
        Event event = Event.builder()
                .id(eventId)
                .participantLimit(0)
                .requestModeration(true)
                .build();

        EventRequestStatusUpdateRequest requestDto = EventRequestStatusUpdateRequest.builder()
                .requestsIds(List.of(100L))
                .status(RequestStatus.CONFIRMED)
                .build();

        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.of(event));

        EventUpdateException exception = assertThrows(EventUpdateException.class,
                () -> eventService.changeRequestsStatus(userId, eventId, requestDto));

        assertEquals("Confirmation is not required for events with 0 limit or disabled moderation", exception.getMessage());
        verify(requestRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("Выброс исключения, если пре-модерация отключена")
    void changeRequestsStatus_whenModerationIsDisabled_shouldThrowException() {
        Event event = Event.builder()
                .id(eventId)
                .participantLimit(10)
                .requestModeration(false)
                .build();

        EventRequestStatusUpdateRequest requestDto = EventRequestStatusUpdateRequest.builder()
                .requestsIds(List.of(100L))
                .status(RequestStatus.CONFIRMED)
                .build();

        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.of(event));

        EventUpdateException exception = assertThrows(EventUpdateException.class,
                () -> eventService.changeRequestsStatus(userId, eventId, requestDto));

        assertEquals("Confirmation is not required for events with 0 limit or disabled moderation", exception.getMessage());
    }

    @Test
    @DisplayName("Выброс исключения, если лимит заявок уже достиг максимума")
    void changeRequestsStatus_whenLimitAlreadyReached_shouldThrowException() {
        Event event = Event.builder()
                .id(eventId)
                .participantLimit(5)
                .requestModeration(true)
                .build();

        EventRequestStatusUpdateRequest requestDto = EventRequestStatusUpdateRequest.builder()
                .requestsIds(List.of(100L))
                .status(RequestStatus.CONFIRMED)
                .build();

        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.of(event));
        when(eventStatsCollector.getConReqByEventMap(List.of(event))).thenReturn(Map.of(eventId, 5L));

        EventUpdateException exception = assertThrows(EventUpdateException.class,
                () -> eventService.changeRequestsStatus(userId, eventId, requestDto));

        assertEquals(String.format("The participant limit has been reached for event id = %d", eventId), exception.getMessage());
    }

    @Test
    @DisplayName("Выброс NotFoundException, если не все заявки найдены в БД")
    void changeRequestsStatus_whenSomeRequestsNotFound_shouldThrowNotFoundException() {
        Event event = Event.builder()
                .id(eventId)
                .participantLimit(10)
                .requestModeration(true)
                .build();

        EventRequestStatusUpdateRequest requestDto = EventRequestStatusUpdateRequest.builder()
                .requestsIds(List.of(100L, 101L))
                .status(RequestStatus.CONFIRMED)
                .build();

        ParticipationRequest req1 = ParticipationRequest.builder().id(100L).status(RequestStatus.PENDING).build();

        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.of(event));
        when(eventStatsCollector.getConReqByEventMap(List.of(event))).thenReturn(Collections.emptyMap());
        when(requestRepository.findAllById(Set.of(100L, 101L))).thenReturn(List.of(req1));

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventService.changeRequestsStatus(userId, eventId, requestDto));

        assertEquals(String.format("Some requests were not found among IDs: %s", List.of(100L, 101L)), exception.getMessage());
    }

    @Test
    @DisplayName("Выброс исключения, если статус заявки не PENDING")
    void changeRequestsStatus_whenRequestIsNotPending_shouldThrowException() {
        Event event = Event.builder()
                .id(eventId)
                .participantLimit(10)
                .requestModeration(true)
                .build();

        EventRequestStatusUpdateRequest requestDto = EventRequestStatusUpdateRequest.builder()
                .requestsIds(List.of(100L))
                .status(RequestStatus.CONFIRMED)
                .build();

        ParticipationRequest req = ParticipationRequest.builder().id(100L).status(RequestStatus.CONFIRMED).build();

        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.of(event));
        when(eventStatsCollector.getConReqByEventMap(List.of(event))).thenReturn(Collections.emptyMap());
        when(requestRepository.findAllById(Set.of(100L))).thenReturn(List.of(req));

        EventUpdateException exception = assertThrows(EventUpdateException.class,
                () -> eventService.changeRequestsStatus(userId, eventId, requestDto));

        assertEquals(String.format("Confirmation is required only for events (id = %d) with PENDING status", 100L), exception.getMessage());
    }
}
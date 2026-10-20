package ru.practicum.main.event.service.privateapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.request.dto.ParticipationRequestDto;
import ru.practicum.main.request.dto.mapper.RequestMapper;
import ru.practicum.main.request.model.ParticipationRequest;
import ru.practicum.main.request.model.RequestStatus;
import ru.practicum.main.request.repository.RequestRepository;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServicePrivateGetRequestsByInitiatorUnitTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private RequestMapper requestMapper;

    @InjectMocks
    private EventServicePrivateImpl eventService;

    private final Long userId = 1L;
    private final Long eventId = 10L;

    @Test
    @DisplayName("Успешное получение списка заявок инициатором события")
    void getEventRequestsByInitiator_whenValidInitiatorAndEvent_returnsRequestsList() {
        User user = new User();
        user.setId(userId);

        Event event = new Event();
        event.setId(eventId);
        event.setInitiator(user);

        ParticipationRequest request = ParticipationRequest.builder()
                .id(100L)
                .event(event)
                .requester(user)
                .status(RequestStatus.PENDING)
                .created(LocalDateTime.now())
                .build();

        ParticipationRequestDto requestDto = ParticipationRequestDto.builder()
                .id(100L)
                .eventId(eventId)
                .requesterId(userId)
                .status(RequestStatus.PENDING)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.of(event));
        when(requestRepository.findAllByEventId(eventId)).thenReturn(List.of(request));
        when(requestMapper.toDtoOut(request)).thenReturn(requestDto);

        List<ParticipationRequestDto> result = eventService.getEventRequestsByInitiator(userId, eventId);

        assertThat(result)
                .isNotNull()
                .hasSize(1)
                .containsExactly(requestDto);

        verify(userRepository).findById(userId);
        verify(eventRepository).findByIdAndInitiatorId(eventId, userId);
        verify(requestRepository).findAllByEventId(eventId);
        verify(requestMapper).toDtoOut(request);
    }

    @Test
    @DisplayName("Возврат пустого списка, если на событие нет заявок")
    void getEventRequestsByInitiator_whenNoRequestsFound_returnsEmptyList() {
        User user = new User();
        user.setId(userId);

        Event event = new Event();
        event.setId(eventId);
        event.setInitiator(user);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.of(event));
        when(requestRepository.findAllByEventId(eventId)).thenReturn(Collections.emptyList());

        List<ParticipationRequestDto> result = eventService.getEventRequestsByInitiator(userId, eventId);

        assertThat(result)
                .isNotNull()
                .isEmpty();

        verify(requestRepository).findAllByEventId(eventId);
        verify(requestMapper, never()).toDtoOut(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Выброс NotFoundException, если пользователь не найден")
    void getEventRequestsByInitiator_whenUserNotFound_throwsNotFoundException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEventRequestsByInitiator(userId, eventId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(String.format("User with id = %d not found", userId));

        verify(eventRepository, never()).findByIdAndInitiatorId(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong());
        verify(requestRepository, never()).findAllByEventId(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("Выброс NotFoundException, если событие не найдено или не принадлежит указанному пользователю")
    void getEventRequestsByInitiator_whenEventNotFoundForUser_throwsNotFoundException() {
        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(eventId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEventRequestsByInitiator(userId, eventId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(String.format("Event with id = %d not found for user with id = %d", eventId, userId));

        verify(requestRepository, never()).findAllByEventId(org.mockito.ArgumentMatchers.anyLong());
    }
}
package ru.practicum.main.request.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.model.EventState;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.request.dto.ParticipationRequestDto;
import ru.practicum.main.request.dto.mapper.RequestMapper;
import ru.practicum.main.request.model.ParticipationRequest;
import ru.practicum.main.request.model.RequestStatus;
import ru.practicum.main.request.repository.RequestRepository;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

    @Mock
    private RequestRepository requestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventRepository eventRepository;

    @Spy
    @SuppressWarnings("all")
    private RequestMapper requestMapper = Mappers.getMapper(RequestMapper.class);

    @InjectMocks
    private RequestServiceImpl requestService;

    private User requester;

    private User initiator;

    private Event event;

    @BeforeEach
    void setUp() {

        requester = new User();
        requester.setId(1L);
        requester.setName("Иван");
        requester.setEmail("vanya@gmail.com");

        initiator = new User();
        initiator.setId(2L);
        initiator.setName("Ольга");
        initiator.setEmail("olga@gmail.com");

        event = Event.builder()
                .id(10L)
                .initiator(initiator)
                .build();
    }

    @Test
    void createRequest_whenSuccessful_shouldReturnConfirmedRequestDto() {

        event.setState(EventState.PUBLISHED);
        event.setParticipantLimit(0);
        event.setRequestModeration(false);

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        Mockito.when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        Mockito.when(requestRepository.existsByEventIdAndRequesterId(10L, 1L)).thenReturn(false);

        Mockito.when(requestRepository.save(any(ParticipationRequest.class))).thenAnswer(invocation -> {
            ParticipationRequest req = invocation.getArgument(0);
            req.setId(100L);
            return req;
        });

        ParticipationRequestDto result = requestService.createRequest(1L, 10L);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(RequestStatus.CONFIRMED, result.getStatus());
        assertEquals(1L, result.getRequesterId());
        assertEquals(10L, result.getEventId());
    }

    @Test
    void createRequest_whenRequesterIsInitiator_shouldThrowConflictException() {

        Mockito.when(userRepository.findById(2L)).thenReturn(Optional.of(initiator));
        Mockito.when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        ConflictException exception = assertThrows(ConflictException.class, () -> {
            requestService.createRequest(2L, 10L);
        });

        assertEquals("Организатор события не может подать заявку на участие в нём!", exception.getMessage());
    }

    @Test
    void createRequest_whenLimitReached_shouldThrowConflictException() {

        event.setState(EventState.PUBLISHED);
        event.setParticipantLimit(5);

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        Mockito.when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        Mockito.when(requestRepository.existsByEventIdAndRequesterId(10L, 1L)).thenReturn(false);
        Mockito.when(requestRepository.countByEventIdAndStatus(10L, RequestStatus.CONFIRMED)).thenReturn(5L);

        ConflictException exception = assertThrows(ConflictException.class, () -> {
            requestService.createRequest(1L, 10L);
        });

        assertEquals("На это событие больше нет свободных мест!", exception.getMessage());
    }

    @Test
    void cancelRequest_whenSuccessful_shouldChangeStatusToCanceled() {

        User requester = new User();
        requester.setId(1L);
        requester.setName("Иван");
        requester.setEmail("vanya@gmail.com");

        ParticipationRequest request = ParticipationRequest.builder()
                .id(100L)
                .requester(requester)
                .event(Event.builder().id(10L).build())
                .status(RequestStatus.PENDING)
                .build();

        Mockito.when(requestRepository.findById(100L)).thenReturn(Optional.of(request));
        Mockito.when(requestRepository.save(any(ParticipationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ParticipationRequestDto result = requestService.cancelRequest(1L, 100L);

        assertNotNull(result);
        assertEquals(RequestStatus.CANCELED, result.getStatus());
    }
}


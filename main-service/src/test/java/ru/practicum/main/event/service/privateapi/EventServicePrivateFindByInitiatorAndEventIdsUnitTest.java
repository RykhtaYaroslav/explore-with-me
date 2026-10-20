package ru.practicum.main.event.service.privateapi;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.main.category.dto.CategoryDto;
import ru.practicum.main.category.repository.CategoryRepository;
import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.mapper.EventMapper;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.event.service.EventStatsCollector;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.request.repository.RequestRepository;
import ru.practicum.main.user.dto.UserShortDto;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;
import ru.practicum.main.util.TestDataUtils;
import ru.practicum.stats.client.StatsClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServicePrivateFindByInitiatorAndEventIdsUnitTest {
    private static final Long USER_ID = 100L;
    private static final Long EVENT_ID = 1L;

    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private RequestRepository requestRepository;
    @Mock
    private StatsClient statsRepository;
    @Mock
    private EventStatsCollector eventStatsCollector;

    @Spy
    @SuppressWarnings("all")
    private EventMapper eventMapper = Mappers.getMapper(EventMapper.class);

    @InjectMocks
    private EventServicePrivateImpl eventService;

    private User user;
    private Event event;

    @BeforeEach
    void setUp() {
        EasyRandom easyRandom = TestDataUtils.createEventEasyRandomizer();

        user = easyRandom.nextObject(User.class);
        user.setId(USER_ID);

        event = easyRandom.nextObject(Event.class);
        event.setId(EVENT_ID);
        event.setInitiator(user);
        event.setCreatedOn(LocalDateTime.now().minusDays(1));
    }

    @Test
    @DisplayName("Успешный возврат события со всеми данными")
    void shouldReturnEventFullDtoWithAllData() {
        EventFullDto expectedDto = EventFullDto.builder()
                .id(EVENT_ID)
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .paid(event.getPaid())
                .eventDate(event.getEventDate())
                .category(CategoryDto.builder().id(event.getCategory().getId()).build())
                .initiator(UserShortDto.builder().id(USER_ID).build())
                .views(999L)
                .confirmedRequests(1L)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(EVENT_ID, USER_ID)).thenReturn(Optional.of(event));
        when(eventStatsCollector.getFullDtoListWithStats(List.of(event))).thenReturn(List.of(expectedDto));

        EventFullDto result = eventService.findByInitiatorAndEventIds(USER_ID, EVENT_ID);

        assertThat(result).isNotNull().isEqualTo(expectedDto);

        verify(userRepository, times(1)).findById(USER_ID);
        verify(eventRepository, times(1)).findByIdAndInitiatorId(EVENT_ID, USER_ID);
        verify(eventStatsCollector, times(1)).getFullDtoListWithStats(List.of(event));

        verifyNoMoreInteractions(userRepository, eventRepository, eventStatsCollector);
        verifyNoInteractions(categoryRepository, statsRepository, requestRepository);
    }

    @Test
    @DisplayName("Успешный возврат события с нулевой статистикой")
    void shouldReturnEventWithZeroStatistics() {
        EventFullDto expectedDto = EventFullDto.builder()
                .id(EVENT_ID)
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .paid(event.getPaid())
                .eventDate(event.getEventDate())
                .category(CategoryDto.builder().id(event.getCategory().getId()).build())
                .initiator(UserShortDto.builder().id(USER_ID).build())
                .views(0L)
                .confirmedRequests(0L)
                .build();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(EVENT_ID, USER_ID)).thenReturn(Optional.of(event));
        when(eventStatsCollector.getFullDtoListWithStats(List.of(event))).thenReturn(List.of(expectedDto));

        EventFullDto result = eventService.findByInitiatorAndEventIds(USER_ID, EVENT_ID);

        assertThat(result).isNotNull().isEqualTo(expectedDto);

        verify(userRepository, times(1)).findById(USER_ID);
        verify(eventRepository, times(1)).findByIdAndInitiatorId(EVENT_ID, USER_ID);
        verify(eventStatsCollector, times(1)).getFullDtoListWithStats(List.of(event));

        verifyNoMoreInteractions(userRepository, eventRepository, eventStatsCollector);
        verifyNoInteractions(categoryRepository, statsRepository, requestRepository);
    }

    @Test
    @DisplayName("NotFoundException если пользователь не найден")
    void shouldThrowNotFoundExceptionWhenUserNotFound() {
        Mockito.when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findByInitiatorAndEventIds(USER_ID, EVENT_ID))
                .isInstanceOf(NotFoundException.class);

        verify(userRepository, times(1)).findById(USER_ID);

        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(eventRepository, categoryRepository, statsRepository, requestRepository, eventStatsCollector);
    }

    @Test
    @DisplayName("NotFoundException если у пользователя не найден ивент")
    void shouldThrowNotFoundExceptionWhenUserHasNoSuchEvent() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(eventRepository.findByIdAndInitiatorId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findByInitiatorAndEventIds(USER_ID, EVENT_ID))
                .isInstanceOf(NotFoundException.class);

        verify(userRepository, times(1)).findById(USER_ID);
        verify(eventRepository, times(1)).findByIdAndInitiatorId(EVENT_ID, USER_ID);

        verifyNoMoreInteractions(userRepository, eventRepository);
        verifyNoInteractions(categoryRepository, statsRepository, requestRepository, eventStatsCollector);
    }
}
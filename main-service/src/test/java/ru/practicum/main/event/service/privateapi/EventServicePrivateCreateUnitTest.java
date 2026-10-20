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
import ru.practicum.main.category.model.Category;
import ru.practicum.main.category.repository.CategoryRepository;
import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.NewEventDto;
import ru.practicum.main.event.dto.mapper.EventMapper;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.model.EventState;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.event.service.EventStatsCollector;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.request.repository.RequestRepository;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;
import ru.practicum.main.util.TestDataUtils;
import ru.practicum.stats.client.StatsClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class EventServicePrivateCreateUnitTest {
    private static final Long USER_ID = 100L;
    private static final Long CATEGORY_ID = 10L;
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
    private Category category;
    private NewEventDto newEventDto;

    @BeforeEach
    void setUp() {
        EasyRandom easyRandom = TestDataUtils.createEventEasyRandomizer();
        user = easyRandom.nextObject(User.class);
        user.setId(USER_ID);

        category = easyRandom.nextObject(Category.class);
        category.setId(CATEGORY_ID);

        newEventDto = easyRandom.nextObject(NewEventDto.class);
        newEventDto.setCategoryId(CATEGORY_ID);
    }

    @Test
    @DisplayName("Успешное создание нового ивента из DTO со всеми полями")
    void shouldCreateNewEventFromAllFields() {
        Mockito.when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        Mockito.when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        Mockito.when(eventRepository.save(Mockito.any(Event.class))).thenAnswer(invocationOnMock -> {
            Event eventToSave = invocationOnMock.getArgument(0);
            eventToSave.setId(EVENT_ID);
            return eventToSave;
        });

        EventFullDto result = eventService.create(USER_ID, newEventDto);

        assertThat(result)
                .isNotNull()
                .returns(EVENT_ID, EventFullDto::getId)
                .returns(newEventDto.getTitle(), EventFullDto::getTitle)
                .returns(EventState.PENDING, EventFullDto::getState)
                .returns(0L, EventFullDto::getConfirmedRequests)
                .returns(0L, EventFullDto::getViews)
                .returns(USER_ID, dto -> dto.getInitiator().getId())
                .returns(newEventDto.getParticipantLimit(), EventFullDto::getParticipantLimit)
                .returns(CATEGORY_ID, dto -> dto.getCategory().getId());

        Mockito.verify(userRepository, Mockito.times(1)).findById(USER_ID);
        Mockito.verify(categoryRepository, Mockito.times(1)).findById(CATEGORY_ID);
        Mockito.verify(eventRepository, Mockito.times(1)).save(Mockito.any(Event.class));

        Mockito.verifyNoMoreInteractions(userRepository, categoryRepository, eventRepository);
        Mockito.verifyNoInteractions(statsRepository, requestRepository, eventStatsCollector);
    }

    @Test
    @DisplayName("Ошибка если юзер не найден")
    void shouldThrowNotFoundExceptionWhenUserNotFound() {
        Mockito.when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.create(USER_ID, newEventDto))
                .isInstanceOf(NotFoundException.class);

        Mockito.verify(userRepository, Mockito.times(1)).findById(USER_ID);
        Mockito.verifyNoMoreInteractions(userRepository);
        Mockito.verifyNoInteractions(eventRepository, categoryRepository, statsRepository, requestRepository, eventStatsCollector);
    }

    @Test
    @DisplayName("Ошибка, если категория не найдена")
    void shouldThrowNotFoundExceptionWhenCategoryNotFound() {
        Mockito.when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        Mockito.when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.create(USER_ID, newEventDto))
                .isInstanceOf(NotFoundException.class);

        Mockito.verify(userRepository, Mockito.times(1)).findById(USER_ID);
        Mockito.verify(categoryRepository, Mockito.times(1)).findById(CATEGORY_ID);

        Mockito.verifyNoMoreInteractions(userRepository, categoryRepository);
        Mockito.verifyNoInteractions(eventRepository, statsRepository, requestRepository, eventStatsCollector);
    }
}
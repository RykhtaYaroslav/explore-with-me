package ru.practicum.core.event.service.privateapi;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.core.category.model.Category;
import ru.practicum.core.category.repository.CategoryRepository;
import ru.practicum.core.common.exception.EventUpdateException;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.event.dto.LocationDto;
import ru.practicum.core.event.dto.mapper.EventMapper;
import ru.practicum.core.event.dto.request.NewEventDto;
import ru.practicum.core.event.dto.request.StateActionUser;
import ru.practicum.core.event.dto.request.UpdateEventUserRequest;
import ru.practicum.core.event.dto.response.EventFullDto;
import ru.practicum.core.event.model.Event;
import ru.practicum.core.event.model.EventState;
import ru.practicum.core.event.repository.EventRepository;
import ru.practicum.core.event.service.EventStatsCollector;
import ru.practicum.core.event.service.interservice.RecommendationInterservice;
import ru.practicum.core.event.service.interservice.UserInterservice;
import ru.practicum.core.event.util.TestDataUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServicePrivateImplTest {
    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserInterservice userInterservice;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private EventStatsCollector statsCollector;
    @Mock
    private RecommendationInterservice recommendations;
    @InjectMocks
    private EventServicePrivateImpl service;
    private EventMapper mapper;
    private EasyRandom random;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(EventMapper.class);
        service = new EventServicePrivateImpl(eventRepository, userInterservice, categoryRepository, mapper,
                statsCollector, recommendations);
        random = TestDataUtils.createEventEasyRandomizer();
    }

    @AfterEach
    void verifyNoMoreMockInteractions() {
        verifyNoMoreInteractions(eventRepository, userInterservice, categoryRepository, statsCollector, recommendations);
    }

    @Test
    @DisplayName("Создает событие со статусом PENDING и нулевой статистикой")
    void createsEvent() {
        long userId = 21L;
        Category category = category(8L);
        NewEventDto request = random.nextObject(NewEventDto.class);
        request.setCategoryId(category.getId());
        request.setEventDate(LocalDateTime.now().plusDays(5));
        var initiator = random.nextObject(ru.practicum.core.user.dto.UserShortDto.class);
        initiator.setId(userId);
        when(userInterservice.getShortById(userId)).thenReturn(initiator);
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            event.setId(99L);
            return event;
        });

        EventFullDto result = service.create(userId, request);

        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getTitle()).isEqualTo(request.getTitle());
        assertThat(result.getState()).isEqualTo(EventState.PENDING);
        assertThat(result.getInitiator()).isSameAs(initiator);
        assertThat(result.getCategory().getId()).isEqualTo(category.getId());
        assertThat(result.getConfirmedRequests()).isZero();
        assertThat(result.getViews()).isZero();
        assertThat(result.getRating()).isEqualTo(5.0);
        verify(categoryRepository, times(1)).findById(category.getId());
        verify(eventRepository, times(1)).save(any(Event.class));
        verify(userInterservice, times(1)).getShortById(userId);
        verifyNoInteractions(statsCollector);
    }

    @Test
    @DisplayName("Не создает событие, если категория не найдена")
    void createMissingCategory() {
        NewEventDto request = random.nextObject(NewEventDto.class);
        request.setCategoryId(73L);
        when(userInterservice.getShortById(21L)).thenReturn(user(21L));
        assertThatThrownBy(() -> service.create(21L, request)).isInstanceOf(NotFoundException.class);
        verify(categoryRepository, times(1)).findById(73L);
        verify(eventRepository, times(0)).save(any());
        verify(userInterservice, times(1)).getShortById(21L);
        verifyNoInteractions(statsCollector);
    }

    @Test
    @DisplayName("Не создает событие, если инициатор не найден")
    void createMissingInitiator() {
        doThrow(new NotFoundException("missing user")).when(userInterservice).getShortById(21L);
        assertThatThrownBy(() -> service.create(21L, random.nextObject(NewEventDto.class)))
                .isInstanceOf(NotFoundException.class).hasMessage("missing user");
        verify(userInterservice, times(1)).getShortById(21L);
        verifyNoInteractions(categoryRepository, eventRepository, statsCollector);
    }

    @Test
    @DisplayName("Частично обновляет событие и переводит его на модерацию")
    void updatesTitleAndSendsToReview() {
        Event event = event(17L, 21L, EventState.PENDING, LocalDateTime.now().plusDays(3));
        when(eventRepository.findByIdAndInitiatorId(17L, 21L)).thenReturn(Optional.of(event));
        when(statsCollector.getFullDtoListWithStats(any())).thenReturn(List.of(random.nextObject(EventFullDto.class)));
        String oldDescription = event.getDescription();
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setTitle("New event title");
        request.setStateAction(StateActionUser.SEND_TO_REVIEW);

        service.updateEventByInitiator(request, 21L, 17L);

        assertThat(event.getTitle()).isEqualTo("New event title");
        assertThat(event.getDescription()).isEqualTo(oldDescription);
        assertThat(event.getState()).isEqualTo(EventState.PENDING);
        verify(eventRepository, times(1)).findByIdAndInitiatorId(17L, 21L);
        verify(eventRepository, times(1)).save(event);
        verify(statsCollector, times(1)).getFullDtoListWithStats(List.of(event));
        verify(userInterservice, times(1)).checkExistsByIdOrThrow(21L);
        verifyNoInteractions(categoryRepository);
    }

    @Test
    @DisplayName("Обновляет переданные поля события и его категорию")
    void updatesAllProvidedFieldsAndCategory() {
        Event event = event(17L, 21L, EventState.PENDING, LocalDateTime.now().plusDays(3));
        Category newCategory = category(13L);
        when(eventRepository.findByIdAndInitiatorId(17L, 21L)).thenReturn(Optional.of(event));
        when(categoryRepository.findById(13L)).thenReturn(Optional.of(newCategory));
        when(statsCollector.getFullDtoListWithStats(any())).thenReturn(List.of(random.nextObject(EventFullDto.class)));
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setTitle("Updated title");
        request.setAnnotation("Updated annotation with enough text");
        request.setDescription("Updated event description");
        request.setCategoryId(13L);
        request.setEventDate(LocalDateTime.now().plusDays(4));
        request.setLocation(new LocationDto(50.0, 45.0));
        request.setPaid(true);
        request.setParticipantLimit(6);
        request.setRequestModeration(false);
        request.setStateAction(StateActionUser.CANCEL_REVIEW);

        service.updateEventByInitiator(request, 21L, 17L);

        assertThat(event.getTitle()).isEqualTo(request.getTitle());
        assertThat(event.getAnnotation()).isEqualTo(request.getAnnotation());
        assertThat(event.getDescription()).isEqualTo(request.getDescription());
        assertThat(event.getCategory()).isSameAs(newCategory);
        assertThat(event.getEventDate()).isEqualTo(request.getEventDate());
        assertThat(event.getLocation().getLat()).isEqualTo(45.0);
        assertThat(event.getLocation().getLon()).isEqualTo(50.0);
        assertThat(event.getPaid()).isTrue();
        assertThat(event.getParticipantLimit()).isEqualTo(6);
        assertThat(event.getRequestModeration()).isFalse();
        assertThat(event.getState()).isEqualTo(EventState.CANCELED);
        verify(eventRepository, times(1)).findByIdAndInitiatorId(17L, 21L);
        verify(eventRepository, times(1)).save(event);
        verify(statsCollector, times(1)).getFullDtoListWithStats(List.of(event));
        verify(userInterservice, times(1)).checkExistsByIdOrThrow(21L);
    }

    @Test
    @DisplayName("Событие без полей обновления сохраняет поля и статистику")
    void nullUpdateKeepsEvent() {
        Event event = event(17L, 21L, EventState.CANCELED, LocalDateTime.now().plusDays(3));
        String title = event.getTitle();
        when(eventRepository.findByIdAndInitiatorId(17L, 21L)).thenReturn(Optional.of(event));
        EventFullDto output = random.nextObject(EventFullDto.class);
        when(statsCollector.getFullDtoListWithStats(any())).thenReturn(List.of(output));

        assertThat(service.updateEventByInitiator(new UpdateEventUserRequest(), 21L, 17L)).isSameAs(output);
        assertThat(event.getTitle()).isEqualTo(title);
        assertThat(event.getState()).isEqualTo(EventState.CANCELED);
        verify(eventRepository, times(1)).findByIdAndInitiatorId(17L, 21L);
        verify(eventRepository, times(1)).save(event);
        verify(statsCollector, times(1)).getFullDtoListWithStats(List.of(event));
        verify(userInterservice, times(1)).checkExistsByIdOrThrow(21L);
        verifyNoInteractions(categoryRepository);
    }

    @Test
    @DisplayName("Не обновляет опубликованное событие")
    void rejectsPublishedEventUpdate() {
        when(eventRepository.findByIdAndInitiatorId(17L, 21L))
                .thenReturn(Optional.of(event(17L, 21L, EventState.PUBLISHED, LocalDateTime.now().plusDays(3))));
        assertThatThrownBy(() -> service.updateEventByInitiator(new UpdateEventUserRequest(), 21L, 17L))
                .isInstanceOf(EventUpdateException.class);
        verify(eventRepository, times(1)).findByIdAndInitiatorId(17L, 21L);
        verify(eventRepository, times(0)).save(any());
        verify(userInterservice, times(1)).checkExistsByIdOrThrow(21L);
        verifyNoInteractions(categoryRepository, statsCollector);
    }

    @Test
    @DisplayName("Разрешает перенести событие, дата которого уже ближе двух часов")
    void allowsChangingNearEventDate() {
        Event event = event(17L, 21L, EventState.PENDING, LocalDateTime.now().plusMinutes(30));
        when(eventRepository.findByIdAndInitiatorId(17L, 21L)).thenReturn(Optional.of(event));
        when(statsCollector.getFullDtoListWithStats(any())).thenReturn(List.of(random.nextObject(EventFullDto.class)));
        LocalDateTime replacement = LocalDateTime.now().plusDays(2);
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setEventDate(replacement);

        service.updateEventByInitiator(request, 21L, 17L);

        assertThat(event.getEventDate()).isEqualTo(replacement);
        verify(eventRepository, times(1)).findByIdAndInitiatorId(17L, 21L);
        verify(eventRepository, times(1)).save(event);
        verify(statsCollector, times(1)).getFullDtoListWithStats(List.of(event));
        verify(userInterservice, times(1)).checkExistsByIdOrThrow(21L);
        verifyNoInteractions(categoryRepository);
    }

    @Test
    @DisplayName("Не обновляет событие, начинающееся менее чем через два часа, без новой даты")
    void rejectsNearEventWithoutNewDate() {
        when(eventRepository.findByIdAndInitiatorId(17L, 21L))
                .thenReturn(Optional.of(event(17L, 21L, EventState.PENDING, LocalDateTime.now().plusMinutes(30))));
        assertThatThrownBy(() -> service.updateEventByInitiator(new UpdateEventUserRequest(), 21L, 17L))
                .isInstanceOf(EventUpdateException.class);
        verify(eventRepository, times(1)).findByIdAndInitiatorId(17L, 21L);
        verify(eventRepository, times(0)).save(any());
        verify(userInterservice, times(1)).checkExistsByIdOrThrow(21L);
        verifyNoInteractions(categoryRepository, statsCollector);
    }

    @Test
    @DisplayName("Не обновляет событие, если запрошенная категория не найдена")
    void updateMissingCategory() {
        Event event = event(17L, 21L, EventState.PENDING, LocalDateTime.now().plusDays(3));
        when(eventRepository.findByIdAndInitiatorId(17L, 21L)).thenReturn(Optional.of(event));
        when(categoryRepository.findById(404L)).thenReturn(Optional.empty());
        UpdateEventUserRequest request = new UpdateEventUserRequest();
        request.setCategoryId(404L);

        assertThatThrownBy(() -> service.updateEventByInitiator(request, 21L, 17L))
                .isInstanceOf(NotFoundException.class);
        verify(eventRepository, times(1)).findByIdAndInitiatorId(17L, 21L);
        verify(categoryRepository, times(1)).findById(404L);
        verify(eventRepository, times(0)).save(any());
        verify(userInterservice, times(1)).checkExistsByIdOrThrow(21L);
        verifyNoInteractions(statsCollector);
    }

    @Test
    @DisplayName("Не возвращает чужое событие по запросу инициатора")
    void missingOwnedEvent() {
        when(eventRepository.findByIdAndInitiatorId(17L, 21L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findByInitiatorAndEventIds(21L, 17L))
                .isInstanceOf(NotFoundException.class);
        verify(eventRepository, times(1)).findByIdAndInitiatorId(17L, 21L);
        verify(userInterservice, times(1)).checkExistsByIdOrThrow(21L);
        verifyNoInteractions(statsCollector);
        verifyNoInteractions(categoryRepository);
    }

    @Test
    @DisplayName("Возвращает полное событие инициатора со статистикой")
    void getsFullEvent() {
        Event event = event(17L, 21L, EventState.PUBLISHED, LocalDateTime.now().plusDays(3));
        EventFullDto output = random.nextObject(EventFullDto.class);
        when(eventRepository.findByIdAndInitiatorId(17L, 21L)).thenReturn(Optional.of(event));
        when(statsCollector.getFullDtoListWithStats(List.of(event))).thenReturn(List.of(output));

        assertThat(service.findByInitiatorAndEventIds(21L, 17L)).isSameAs(output);
        verify(eventRepository, times(1)).findByIdAndInitiatorId(17L, 21L);
        verify(statsCollector, times(1)).getFullDtoListWithStats(List.of(event));
        verify(userInterservice, times(1)).checkExistsByIdOrThrow(21L);
        verifyNoInteractions(categoryRepository);
    }

    @Test
    @DisplayName("Возвращает полное событие с нулевой статистикой")
    void getsFullEventWithZeroStats() {
        Event event = event(17L, 21L, EventState.PUBLISHED, LocalDateTime.now().plusDays(3));
        when(eventRepository.findByIdAndInitiatorId(17L, 21L)).thenReturn(Optional.of(event));
        EventFullDto output = random.nextObject(EventFullDto.class);
        when(statsCollector.getFullDtoListWithStats(List.of(event))).thenReturn(List.of(output));

        assertThat(service.findByInitiatorAndEventIds(21L, 17L)).isSameAs(output);

        verify(userInterservice).checkExistsByIdOrThrow(21L);
        verify(eventRepository).findByIdAndInitiatorId(17L, 21L);
        verify(statsCollector).getFullDtoListWithStats(List.of(event));
        verifyNoInteractions(categoryRepository);
    }

    @Test
    @DisplayName("Не ищет событие, если инициатор не найден")
    void missingInitiatorForEventLookup() {
        doThrow(new NotFoundException("missing user")).when(userInterservice).checkExistsByIdOrThrow(21L);

        assertThatThrownBy(() -> service.findByInitiatorAndEventIds(21L, 17L))
                .isInstanceOf(NotFoundException.class).hasMessage("missing user");

        verify(userInterservice).checkExistsByIdOrThrow(21L);
        verifyNoInteractions(eventRepository, statsCollector, categoryRepository);
    }

    private Category category(Long id) {
        Category category = random.nextObject(Category.class);
        category.setId(id);
        return category;
    }

    private ru.practicum.core.user.dto.UserShortDto user(Long id) {
        var user = random.nextObject(ru.practicum.core.user.dto.UserShortDto.class);
        user.setId(id);
        return user;
    }

    private Event event(Long id, Long userId, EventState state, LocalDateTime date) {
        Event event = random.nextObject(Event.class);
        event.setId(id);
        event.setInitiatorId(userId);
        event.setState(state);
        event.setEventDate(date);
        event.setCreatedOn(LocalDateTime.now().minusDays(2));
        return event;
    }
}

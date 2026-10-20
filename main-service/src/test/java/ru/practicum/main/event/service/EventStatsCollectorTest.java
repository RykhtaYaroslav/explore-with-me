package ru.practicum.main.event.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.main.event.dto.mapper.EventMapper;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.request.dto.ConfirmedRequestsCount;
import ru.practicum.main.request.model.RequestStatus;
import ru.practicum.main.request.repository.RequestRepository;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventStatsCollectorTest {

    @Mock
    private StatsClient statsRepository;

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private EventMapper eventMapper;

    @InjectMocks
    private EventStatsCollectorImpl statsCollector;

    private Event event1;
    private Event event2;

    private static final Long EVENT_1_ID = 10L;
    private static final Long EVENT_2_ID = 20L;

    @BeforeEach
    void setUp() {
        event1 = Event.builder()
                .id(EVENT_1_ID)
                .createdOn(LocalDateTime.now().minusDays(5))
                .build();

        event2 = Event.builder()
                .id(EVENT_2_ID)
                .createdOn(LocalDateTime.now().minusDays(2))
                .build();
    }

    @Test
    @DisplayName("getViewsByEventMap: корректное извлечение просмотров и парсинг URI")
    void getViewsByEventMap_shouldReturnCorrectMap() {
        List<Event> events = List.of(event1, event2);
        List<String> expectedUris = List.of("/events/10", "/events/20");

        ViewStatsDto stats1 = new ViewStatsDto("main-service", "/events/10", 150L);
        ViewStatsDto stats2 = new ViewStatsDto("main-service", "/events/20", 300L);

        when(statsRepository.getStats(
                argThat(minDate -> minDate.isEqual(event1.getCreatedOn()) || minDate.isBefore(event1.getCreatedOn())),
                any(LocalDateTime.class),
                eq(expectedUris),
                eq(true)
        )).thenReturn(List.of(stats1, stats2));

        Map<Long, Long> result = statsCollector.getViewsByEventMap(events);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(2, result.size()),
                () -> assertEquals(150L, result.get(EVENT_1_ID)),
                () -> assertEquals(300L, result.get(EVENT_2_ID))
        );

        verify(statsRepository).getStats(any(), any(), eq(expectedUris), eq(true));
    }

    @Test
    @DisplayName("getConReqByEventMap: подтягивание подтвержденных заявок из БД")
    void getConReqByEventMap_shouldReturnConfirmedRequestsCount() {
        List<Event> events = List.of(event1, event2);
        List<Long> eventIds = List.of(EVENT_1_ID, EVENT_2_ID);

        ConfirmedRequestsCount count1 = new ConfirmedRequestsCount(EVENT_1_ID, 5L);
        ConfirmedRequestsCount count2 = new ConfirmedRequestsCount(EVENT_2_ID, 12L);

        when(requestRepository.countRequestsCountByEventIds(eventIds, RequestStatus.CONFIRMED))
                .thenReturn(List.of(count1, count2));

        Map<Long, Long> result = statsCollector.getConReqByEventMap(events);

        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(2, result.size()),
                () -> assertEquals(5L, result.get(EVENT_1_ID)),
                () -> assertEquals(12L, result.get(EVENT_2_ID))
        );

        verify(requestRepository).countRequestsCountByEventIds(eventIds, RequestStatus.CONFIRMED);
    }

//    @Test
//    @DisplayName("getShortDtoListWithStats: обогащение списка объектов EventShortDto")
//    void getShortDtoListWithStats_shouldEnrichShortDtos() {
//        List<Event> events = List.of(event1);
//        List<String> uris = List.of("/events/10");
//
//        ViewStatsDto viewStats = new ViewStatsDto("main-service", "/events/10", 100L);
//        ConfirmedRequestsCount reqCount = new ConfirmedRequestsCount(EVENT_1_ID, 3L);
//
//        EventShortDto expectedDto = EventShortDto.builder()
//                .id(EVENT_1_ID)
//                .views(100L)
//                .confirmedRequests(3L)
//                .build();
//
//        when(statsRepository.getStats(any(), any(), eq(uris), eq(true)))
//                .thenReturn(List.of(viewStats));
//        when(requestRepository.countRequestsCountByEventIds(List.of(EVENT_1_ID), RequestStatus.CONFIRMED))
//                .thenReturn(List.of(reqCount));
//        when(eventMapper.toShortDto(event1, 100L, 3L))
//                .thenReturn(expectedDto);
//
//        List<EventShortDto> result = statsCollector.getShortDtoListWithStats(events);
//
//        assertThat(result)
//                .hasSize(1)
//                .containsExactly(expectedDto);
//
//        verify(eventMapper).toShortDto(event1, 100L, 3L);
//    }

//    @Test
//    @DisplayName("getFullDtoListWithStats: обогащение списка объектов EventFullDto")
//    void getFullDtoListWithStats_shouldEnrichFullDtos() {
//        List<Event> events = List.of(event1);
//        List<String> uris = List.of("/events/10");
//
//        ViewStatsDto viewStats = new ViewStatsDto("main-service", "/events/10", 250L);
//        ConfirmedRequestsCount reqCount = new ConfirmedRequestsCount(EVENT_1_ID, 10L);
//
//        EventFullDto expectedDto = EventFullDto.builder()
//                .id(EVENT_1_ID)
//                .views(250L)
//                .confirmedRequests(10L)
//                .build();
//
//        when(statsRepository.getStats(any(), any(), eq(uris), eq(true)))
//                .thenReturn(List.of(viewStats));
//        when(requestRepository.countRequestsCountByEventIds(List.of(EVENT_1_ID), RequestStatus.CONFIRMED))
//                .thenReturn(List.of(reqCount));
//        when(eventMapper.toFullDto(event1, 250L, 10L))
//                .thenReturn(expectedDto);
//
//        List<EventFullDto> result = statsCollector.getFullDtoListWithStats(events);
//
//        assertThat(result)
//                .hasSize(1)
//                .containsExactly(expectedDto);
//
//        verify(eventMapper).toFullDto(event1, 250L, 10L);
//    }

    @Test
    @DisplayName("extractIdFromUri: корректный парсинг числового ID из конца строки URI")
    void extractIdFromUri_shouldExtractLongId() {
        Event testEvent = Event.builder()
                .id(10L)
                .createdOn(LocalDateTime.now().minusDays(1))
                .build();

        when(statsRepository.getStats(any(), any(), any(), eq(true)))
                .thenReturn(List.of(new ViewStatsDto("main-service", "/events/10", 100L)));

        Map<Long, Long> result = statsCollector.getViewsByEventMap(List.of(testEvent));

        assertThat(result)
                .containsEntry(10L, 100L);
    }

    @Test
    @DisplayName("getViewsByEventMap: использование дефолтной даты (-10 лет), если createdOn равен null")
    void getViewsByEventMap_whenCreatedOnIsNull_shouldUseDefaultMinDate() {
        Event eventWithoutDate = Event.builder()
                .id(99L)
                .createdOn(null)
                .build();

        List<String> uris = List.of("/events/99");

        when(statsRepository.getStats(any(LocalDateTime.class), any(LocalDateTime.class), eq(uris), eq(true)))
                .thenReturn(List.of());

        statsCollector.getViewsByEventMap(List.of(eventWithoutDate));

        verify(statsRepository).getStats(
                argThat(minDate -> minDate != null && minDate.isBefore(LocalDateTime.now().minusYears(9))),
                any(LocalDateTime.class),
                eq(uris),
                eq(true)
        );
    }
}
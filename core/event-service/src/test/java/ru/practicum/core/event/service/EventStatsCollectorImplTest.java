package ru.practicum.core.event.service;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.core.event.dto.EventRatingCount;
import ru.practicum.core.event.dto.mapper.EventMapper;
import ru.practicum.core.event.dto.response.EventFullDto;
import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.core.event.model.Event;
import ru.practicum.core.event.service.interservice.ReviewInterservice;
import ru.practicum.core.event.util.TestDataUtils;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class EventStatsCollectorImplTest {
    @Mock
    private StatsClient statsClient;
    @Mock
    private ReviewInterservice reviewInterservice;

    private EventStatsCollectorImpl collector;
    private EasyRandom random;

    @BeforeEach
    void setUp() {
        collector = new EventStatsCollectorImpl(statsClient, reviewInterservice, Mappers.getMapper(EventMapper.class));
        random = TestDataUtils.createEventEasyRandomizer();
    }

    @AfterEach
    void verifyNoMoreMockInteractions() {
        verifyNoMoreInteractions(statsClient, reviewInterservice);
    }

    @Test
    @DisplayName("getViewsByEventMap queries event URIs and maps hit counts by the trailing ID")
    void getViewsMapsUriIds() {
        Event first = event(41L, LocalDateTime.of(2026, 1, 1, 10, 0));
        Event second = event(42L, LocalDateTime.of(2026, 1, 2, 10, 0));
        when(statsClient.getStats(org.mockito.ArgumentMatchers.eq(first.getCreatedOn()),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.eq(List.of("/events/41", "/events/42")),
                org.mockito.ArgumentMatchers.eq(true)))
                .thenReturn(List.of(new ViewStatsDto("app", "/events/group/41", 7L),
                        new ViewStatsDto("app", "/events/42", 12L)));

        Map<Long, Long> result = collector.getViewsByEventMap(List.of(first, second));

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(41L, 7L, 42L, 12L));
        verify(statsClient, times(1)).getStats(org.mockito.ArgumentMatchers.eq(first.getCreatedOn()),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.eq(List.of("/events/41", "/events/42")),
                org.mockito.ArgumentMatchers.eq(true));
        verifyNoInteractions(reviewInterservice);
    }

    @Test
    @DisplayName("getViewsByEventMap uses ten years ago when all creation dates are absent")
    void getViewsUsesFallbackMinimumDate() {
        Event event = event(31L, null);
        LocalDateTime beforeCall = LocalDateTime.now();
        when(statsClient.getStats(org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.eq(List.of("/events/31")), org.mockito.ArgumentMatchers.eq(true)))
                .thenReturn(List.of(new ViewStatsDto("app", "/events/31", 5L)));

        collector.getViewsByEventMap(List.of(event));

        org.mockito.ArgumentCaptor<LocalDateTime> start = org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
        org.mockito.ArgumentCaptor<LocalDateTime> end = org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
        verify(statsClient, times(1)).getStats(start.capture(), end.capture(),
                org.mockito.ArgumentMatchers.eq(List.of("/events/31")), org.mockito.ArgumentMatchers.eq(true));
        assertThat(start.getValue()).isAfter(beforeCall.minusYears(10).minusSeconds(2))
                .isBefore(beforeCall.minusYears(10).plusSeconds(2));
        assertThat(end.getValue()).isAfterOrEqualTo(beforeCall);
        verifyNoInteractions(reviewInterservice);
    }

    @Test
    @DisplayName("getConReqByEventMap maps each event to its confirmed request count")
    void getConfirmedCounts() {
        Event first = event(1L, null);
        first.setConfirmedRequests(8L);
        Event second = event(2L, null);
        second.setConfirmedRequests(3L);

        assertThat(collector.getConReqByEventMap(List.of(first, second)))
                .containsExactlyInAnyOrderEntriesOf(Map.of(1L, 8L, 2L, 3L));
        verifyNoInteractions(statsClient, reviewInterservice);
    }

    @Test
    @DisplayName("getShortDtoListWithStats enriches short DTOs with views, requests, and ratings")
    void enrichShortDtos() {
        Event first = event(12L, LocalDateTime.of(2026, 1, 1, 0, 0));
        first.setConfirmedRequests(4L);
        when(statsClient.getStats(org.mockito.ArgumentMatchers.eq(first.getCreatedOn()),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.eq(List.of("/events/12")), org.mockito.ArgumentMatchers.eq(true)))
                .thenReturn(List.of(new ViewStatsDto("app", "/events/12", 33L)));
        when(reviewInterservice.getRatingByIds(List.of(12L))).thenReturn(List.of(new EventRatingCount(12L, 4.25)));

        List<EventShortDto> result = collector.getShortDtoListWithStats(List.of(first));

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo(12L);
            assertThat(dto.getViews()).isEqualTo(33L);
            assertThat(dto.getConfirmedRequests()).isEqualTo(4L);
            assertThat(dto.getRating()).isEqualTo(4.25);
        });
        verify(statsClient, times(1)).getStats(org.mockito.ArgumentMatchers.eq(first.getCreatedOn()),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.eq(List.of("/events/12")), org.mockito.ArgumentMatchers.eq(true));
        verify(reviewInterservice, times(1)).getRatingByIds(List.of(12L));
    }

    @Test
    @DisplayName("getFullDtoListWithStats enriches full DTOs and defaults missing external statistics")
    void enrichFullDtosWithDefaults() {
        Event event = event(15L, LocalDateTime.of(2026, 1, 1, 0, 0));
        event.setConfirmedRequests(2L);
        when(statsClient.getStats(org.mockito.ArgumentMatchers.eq(event.getCreatedOn()),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.eq(List.of("/events/15")), org.mockito.ArgumentMatchers.eq(true)))
                .thenReturn(List.of());
        when(reviewInterservice.getRatingByIds(List.of(15L))).thenReturn(List.of());

        List<EventFullDto> result = collector.getFullDtoListWithStats(List.of(event));

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo(15L);
            assertThat(dto.getViews()).isZero();
            assertThat(dto.getConfirmedRequests()).isEqualTo(2L);
            assertThat(dto.getRating()).isEqualTo(5.0);
        });
        verify(statsClient, times(1)).getStats(org.mockito.ArgumentMatchers.eq(event.getCreatedOn()),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.eq(List.of("/events/15")), org.mockito.ArgumentMatchers.eq(true));
        verify(reviewInterservice, times(1)).getRatingByIds(List.of(15L));
    }

    @Test
    @DisplayName("getViewsByEventMap keeps the maximum when multiple URIs resolve to one event")
    void getViewsUsesMaximumForDuplicateIds() {
        Event event = event(3L, LocalDateTime.of(2026, 1, 1, 0, 0));
        when(statsClient.getStats(org.mockito.ArgumentMatchers.eq(event.getCreatedOn()),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.eq(List.of("/events/3")), org.mockito.ArgumentMatchers.eq(true)))
                .thenReturn(List.of(new ViewStatsDto("app", "/events/3", 2L),
                        new ViewStatsDto("app", "/redirect/3", 9L)));

        assertThat(collector.getViewsByEventMap(List.of(event))).containsEntry(3L, 9L);
        verify(statsClient, times(1)).getStats(org.mockito.ArgumentMatchers.eq(event.getCreatedOn()),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.eq(List.of("/events/3")), org.mockito.ArgumentMatchers.eq(true));
        verifyNoInteractions(reviewInterservice);
    }

    private Event event(Long id, LocalDateTime createdOn) {
        Event event = random.nextObject(Event.class);
        event.setId(id);
        event.setCreatedOn(createdOn);
        return event;
    }
}

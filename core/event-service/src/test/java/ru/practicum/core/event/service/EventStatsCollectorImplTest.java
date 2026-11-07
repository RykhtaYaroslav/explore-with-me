package ru.practicum.core.event.service;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import ru.practicum.core.event.service.interservice.RecommendationInterserviceGrpc;
import ru.practicum.core.event.service.interservice.ReviewInterservice;
import ru.practicum.core.event.service.interservice.UserInterservice;
import ru.practicum.core.event.util.TestDataUtils;
import ru.practicum.core.user.dto.UserShortDto;
import ru.practicum.ewm.stats.model.request.InteractionsCountRequest;
import ru.practicum.ewm.stats.model.response.RecommendedEvent;
import ru.practicum.stats.client.StatsClient;
import ru.practicum.stats.client.grpc.AnalyzerGrpcClient;
import ru.practicum.stats.client.grpc.CollectorGrpcClient;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventStatsCollectorImplTest {
    @Mock
    private StatsClient statsClient;
    @Mock
    private AnalyzerGrpcClient analyzer;
    @Mock
    private CollectorGrpcClient collectorClient;
    @Mock
    private ReviewInterservice reviewInterservice;
    @Mock
    private UserInterservice userInterservice;

    private EventStatsCollectorImpl collector;
    private EasyRandom random;

    @BeforeEach
    void setUp() {
        var recommendations = new RecommendationInterserviceGrpc(collectorClient, analyzer);
        collector = new EventStatsCollectorImpl(statsClient, recommendations, reviewInterservice, userInterservice,
                Mappers.getMapper(EventMapper.class));
        random = TestDataUtils.createEventEasyRandomizer();
    }

    @AfterEach
    void verifyNoMoreMockInteractions() {
        verifyNoMoreInteractions(statsClient, collectorClient, analyzer, reviewInterservice, userInterservice);
    }

    @Test
    @DisplayName("Records the event view in the existing HTTP counter")
    void recordsEventView() {
        collector.recordView(21L, "127.0.0.1");

        verify(statsClient, times(1)).hit(argThat((EndpointHitDto hit) ->
                hit.getUri().equals("/events/21") && hit.getIp().equals("127.0.0.1")
                        && hit.getApp().equals("event-service") && hit.getTimestamp() != null));
        verifyNoInteractions(collectorClient, analyzer, reviewInterservice, userInterservice);
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
        verifyNoInteractions(reviewInterservice, userInterservice);
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
        verifyNoInteractions(reviewInterservice, userInterservice);
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
        verifyNoInteractions(statsClient, reviewInterservice, userInterservice);
    }

    @Test
    @DisplayName("Short cards preserve views and review rating alongside Analyzer weight")
    void enrichShortDtos() {
        Event first = event(12L, LocalDateTime.of(2026, 1, 1, 0, 0));
        first.setConfirmedRequests(4L);
        when(statsClient.getStats(org.mockito.ArgumentMatchers.eq(first.getCreatedOn()),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.eq(List.of("/events/12")), org.mockito.ArgumentMatchers.eq(true)))
                .thenReturn(List.of(new ViewStatsDto("app", "/events/12", 33L)));
        when(reviewInterservice.getRatingByIds(List.of(12L))).thenReturn(List.of(new EventRatingCount(12L, 4.25)));
        var request = new InteractionsCountRequest(List.of(12L));
        when(analyzer.getInteractionsCount(request)).thenReturn(List.of(new RecommendedEvent(12L, 1.8)));
        UserShortDto initiator = user(112L);
        when(userInterservice.getShortByIds(List.of(112L))).thenReturn(List.of(initiator));

        List<EventShortDto> result = collector.getShortDtoListWithStats(List.of(first));

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo(12L);
            assertThat(dto.getViews()).isEqualTo(33L);
            assertThat(dto.getConfirmedRequests()).isEqualTo(4L);
            assertThat(dto.getRating()).isEqualTo(4.25);
            assertThat(dto.getTotalInteractionWeight()).isEqualTo(1.8);
            assertThat(dto.getInitiator()).isSameAs(initiator);
        });
        verify(statsClient, times(1)).getStats(org.mockito.ArgumentMatchers.eq(first.getCreatedOn()),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.eq(List.of("/events/12")), org.mockito.ArgumentMatchers.eq(true));
        verify(reviewInterservice, times(1)).getRatingByIds(List.of(12L));
        verify(analyzer, times(1)).getInteractionsCount(request);
        verify(userInterservice, times(1)).getShortByIds(List.of(112L));
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
        var request = new InteractionsCountRequest(List.of(15L));
        when(analyzer.getInteractionsCount(request)).thenReturn(List.of());
        UserShortDto initiator = user(115L);
        when(userInterservice.getShortByIds(List.of(115L))).thenReturn(List.of(initiator));

        List<EventFullDto> result = collector.getFullDtoListWithStats(List.of(event));

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo(15L);
            assertThat(dto.getViews()).isZero();
            assertThat(dto.getConfirmedRequests()).isEqualTo(2L);
            assertThat(dto.getRating()).isEqualTo(5.0);
            assertThat(dto.getTotalInteractionWeight()).isZero();
            assertThat(dto.getInitiator()).isSameAs(initiator);
        });
        verify(statsClient, times(1)).getStats(org.mockito.ArgumentMatchers.eq(event.getCreatedOn()),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class),
                org.mockito.ArgumentMatchers.eq(List.of("/events/15")), org.mockito.ArgumentMatchers.eq(true));
        verify(reviewInterservice, times(1)).getRatingByIds(List.of(15L));
        verify(analyzer, times(1)).getInteractionsCount(request);
        verify(userInterservice, times(1)).getShortByIds(List.of(115L));
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
        verifyNoInteractions(reviewInterservice, userInterservice);
    }

    private Event event(Long id, LocalDateTime createdOn) {
        Event event = random.nextObject(Event.class);
        event.setId(id);
        event.setInitiatorId(id + 100L);
        event.setCreatedOn(createdOn);
        return event;
    }

    private UserShortDto user(Long id) {
        UserShortDto user = random.nextObject(UserShortDto.class);
        user.setId(id);
        return user;
    }
}

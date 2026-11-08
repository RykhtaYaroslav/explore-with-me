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
import ru.practicum.stats.client.grpc.AnalyzerGrpcClient;
import ru.practicum.stats.client.grpc.CollectorGrpcClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventStatsCollectorImplTest {
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
        collector = new EventStatsCollectorImpl(recommendations, reviewInterservice, userInterservice,
                Mappers.getMapper(EventMapper.class));
        random = TestDataUtils.createEventEasyRandomizer();
    }

    @AfterEach
    void verifyNoMoreMockInteractions() {
        verifyNoMoreInteractions(collectorClient, analyzer, reviewInterservice, userInterservice);
    }

    @Test
    @DisplayName("getConReqByEventMap maps each event to its confirmed request count")
    void getConfirmedCounts() {
        Event first = event(1L);
        first.setConfirmedRequests(8L);
        Event second = event(2L);
        second.setConfirmedRequests(3L);

        assertThat(collector.getConReqByEventMap(List.of(first, second)))
                .containsExactlyInAnyOrderEntriesOf(Map.of(1L, 8L, 2L, 3L));
        verifyNoInteractions(collectorClient, analyzer, reviewInterservice, userInterservice);
    }

    @Test
    @DisplayName("Short cards preserve views and review rating alongside Analyzer weight")
    void enrichShortDtos() {
        Event first = event(12L);
        first.setConfirmedRequests(4L);
        when(reviewInterservice.getRatingByIds(List.of(12L))).thenReturn(List.of(new EventRatingCount(12L, 4.25)));
        var request = new InteractionsCountRequest(List.of(12L));
        when(analyzer.getViewCounts(request)).thenReturn(Map.of(12L, 33L));
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
        verify(analyzer, times(1)).getViewCounts(request);
        verify(reviewInterservice, times(1)).getRatingByIds(List.of(12L));
        verify(analyzer, times(1)).getInteractionsCount(request);
        verify(userInterservice, times(1)).getShortByIds(List.of(112L));
    }

    @Test
    @DisplayName("getFullDtoListWithStats enriches full DTOs and defaults missing external statistics")
    void enrichFullDtosWithDefaults() {
        Event event = event(15L);
        event.setConfirmedRequests(2L);
        when(reviewInterservice.getRatingByIds(List.of(15L))).thenReturn(List.of());
        var request = new InteractionsCountRequest(List.of(15L));
        when(analyzer.getViewCounts(request)).thenReturn(Map.of());
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
        verify(analyzer, times(1)).getViewCounts(request);
        verify(reviewInterservice, times(1)).getRatingByIds(List.of(15L));
        verify(analyzer, times(1)).getInteractionsCount(request);
        verify(userInterservice, times(1)).getShortByIds(List.of(115L));
    }

    private Event event(Long id) {
        Event event = random.nextObject(Event.class);
        event.setId(id);
        event.setInitiatorId(id + 100L);
        return event;
    }

    private UserShortDto user(Long id) {
        UserShortDto user = random.nextObject(UserShortDto.class);
        user.setId(id);
        return user;
    }
}

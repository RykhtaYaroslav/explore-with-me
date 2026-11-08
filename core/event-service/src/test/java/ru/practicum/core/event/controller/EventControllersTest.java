package ru.practicum.core.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.method.HandlerMethod;
import ru.practicum.core.category.repository.CategoryRepository;
import ru.practicum.core.common.exception.CommonErrorHandler;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.event.dto.mapper.EventMapper;
import ru.practicum.core.event.dto.response.EventFullDto;
import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.core.event.model.Event;
import ru.practicum.core.event.model.EventState;
import ru.practicum.core.event.repository.EventRepository;
import ru.practicum.core.event.service.EventStatsCollector;
import ru.practicum.core.event.service.interservice.RecommendationInterserviceGrpc;
import ru.practicum.core.event.service.interservice.UserInterservice;
import ru.practicum.core.event.service.privateapi.EventServicePrivateImpl;
import ru.practicum.core.event.service.publicapi.EventServicePublicImpl;
import ru.practicum.ewm.stats.model.action.ActionType;
import ru.practicum.ewm.stats.model.action.UserAction;
import ru.practicum.ewm.stats.model.request.UserPredictionsRequest;
import ru.practicum.ewm.stats.model.response.RecommendedEvent;
import ru.practicum.stats.client.grpc.AnalyzerGrpcClient;
import ru.practicum.stats.client.grpc.CollectorGrpcClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(value = {PublicEventController.class, PrivateEventController.class}, properties = {
        "spring.cloud.config.enabled=false", "eureka.client.enabled=false"
})
@ContextConfiguration(classes = {PublicEventController.class, PrivateEventController.class,
        EventServicePublicImpl.class, EventServicePrivateImpl.class, RecommendationInterserviceGrpc.class,
        CommonErrorHandler.class})
class EventControllersTest {
    private static final String USER_HEADER = "X-EWM-USER-ID";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @MockBean
    private EventRepository events;
    @MockBean
    private CollectorGrpcClient collector;
    @MockBean
    private AnalyzerGrpcClient analyzer;
    @MockBean
    private EventStatsCollector eventStats;
    @MockBean
    private UserInterservice users;
    @MockBean
    private CategoryRepository categories;
    @MockBean
    private EventMapper eventMapper;

    @AfterEach
    void verifyNoUnexpectedCalls() {
        verifyNoMoreInteractions(events, collector, analyzer, eventStats, users, categories, eventMapper);
    }

    @Test
    @DisplayName("Event search returns cards without recording a view")
    void searchDoesNotSendView() throws Exception {
        Event event = event(21L, EventState.PUBLISHED);
        when(events.findAllWithParams(isNull(), isNull(), isNull(), any(), isNull(),
                eq(false), isNull(), isNull(), eq(List.of("PUBLISHED")), any(Pageable.class)))
                .thenReturn(List.of(event));
        when(eventStats.getShortDtoListWithStats(List.of(event))).thenReturn(List.of(card(21L)));

        var response = response(get("/events"));
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(json.readTree(response.getContentAsByteArray()).get(0).path("id").asLong()).isEqualTo(21L);

        verify(events, times(1)).findAllWithParams(isNull(), isNull(), isNull(), any(), isNull(),
                eq(false), isNull(), isNull(), eq(List.of("PUBLISHED")), any(Pageable.class));
        verify(eventStats, times(1)).getShortDtoListWithStats(List.of(event));
        verifyNoInteractions(collector, analyzer);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"Registered viewer, 67, true", "Unknown viewer, 68, false", "Anonymous viewer, , false"})
    @DisplayName("Event details record views only for registered users")
    void recordsViewForRegisteredUser(String scenario, Long userId, boolean registered) throws Exception {
        Event event = event(21L, EventState.PUBLISHED);
        when(events.findByIdAndState(21L, EventState.PUBLISHED)).thenReturn(Optional.of(event));
        when(eventStats.getFullDtoListWithStats(List.of(event))).thenReturn(List.of(
                EventFullDto.builder().id(21L).views(14L).rating(4.5).totalInteractionWeight(2.8).build()));
        var request = get("/events/21");
        if (userId != null) {
            request.header(USER_HEADER, userId);
            when(users.existsById(userId)).thenReturn(registered);
        }

        var response = response(request);
        assertThat(response.getStatus()).isEqualTo(200);
        var card = json.readTree(response.getContentAsByteArray());
        assertThat(card.path("views").asLong()).isEqualTo(14L);
        assertThat(card.path("rating").asDouble()).isEqualTo(4.5);
        assertThat(card.path("totalInteractionWeight").asDouble()).isEqualTo(2.8);

        verify(events, times(1)).findByIdAndState(21L, EventState.PUBLISHED);
        verify(eventStats, times(1)).getFullDtoListWithStats(List.of(event));
        if (userId != null) {
            verify(users, times(1)).existsById(userId);
        } else {
            verifyNoInteractions(users);
        }
        if (registered) {
            verify(collector, times(1)).collectUserAction(argThat((UserAction action) ->
                    action.userId() == userId && action.eventId() == 21L
                            && action.actionType() == ActionType.VIEW && action.timestamp() != null));
        } else {
            verifyNoInteractions(collector);
        }
        verifyNoInteractions(analyzer);
    }

    @Test
    @DisplayName("An unavailable event does not generate a view")
    void skipsViewsForMissingEvent() throws Exception {
        when(events.findByIdAndState(21L, EventState.PUBLISHED)).thenReturn(Optional.empty());

        assertThat(response(get("/events/21").header(USER_HEADER, "67")).getStatus()).isEqualTo(404);

        verify(events, times(1)).findByIdAndState(21L, EventState.PUBLISHED);
        verifyNoInteractions(users, collector, analyzer, eventStats);
    }

    @Test
    @DisplayName("Recommendations preserve Analyzer order and omit missing and unpublished events")
    void returnsPublishedRecommendationsInOrder() throws Exception {
        var request = new UserPredictionsRequest(67L, 4);
        var ids = List.of(31L, 32L, 33L, 34L);
        Event first = event(31L, EventState.PUBLISHED);
        Event second = event(34L, EventState.PUBLISHED);
        when(analyzer.getRecommendationsForUser(request)).thenReturn(List.of(new RecommendedEvent(31L, 1.0),
                new RecommendedEvent(32L, 0.9), new RecommendedEvent(33L, 0.8), new RecommendedEvent(34L, 0.7)));
        when(events.findAllWithCategoryByIdIn(ids)).thenReturn(List.of(second, event(32L, EventState.PENDING), first));
        when(eventStats.getShortDtoListWithStats(List.of(first, second))).thenReturn(List.of(card(31L), card(34L)));

        var result = mvc.perform(get("/events/recommendations").header(USER_HEADER, "67").param("size", "4"))
                .andReturn();
        assertThat(result.getHandler()).isInstanceOfSatisfying(HandlerMethod.class,
                handler -> assertThat(handler.getBeanType()).isEqualTo(PrivateEventController.class));
        var response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(json.readTree(response.getContentAsByteArray()))
                .extracting(card -> card.path("id").asLong()).containsExactly(31L, 34L);

        var order = inOrder(users, analyzer);
        order.verify(users, times(1)).checkExistsByIdOrThrow(67L);
        order.verify(analyzer, times(1)).getRecommendationsForUser(request);
        verify(events, times(1)).findAllWithCategoryByIdIn(ids);
        verify(eventStats, times(1)).getShortDtoListWithStats(List.of(first, second));
        verifyNoInteractions(collector);
    }

    @Test
    @DisplayName("An empty prediction stream returns an empty array without loading events")
    void returnsEmptyRecommendations() throws Exception {
        var request = new UserPredictionsRequest(67L, 10);
        when(analyzer.getRecommendationsForUser(request)).thenReturn(List.of());

        var response = response(get("/events/recommendations").header(USER_HEADER, "67"));
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).isEqualTo("[]");

        verify(users, times(1)).checkExistsByIdOrThrow(67L);
        verify(analyzer, times(1)).getRecommendationsForUser(request);
        verifyNoInteractions(events, collector, eventStats);
    }

    @Test
    @DisplayName("Recommendations require a valid user ID header")
    void rejectsMissingOrInvalidUser() throws Exception {
        assertThat(response(get("/events/recommendations")).getStatus()).isEqualTo(400);
        assertThat(response(get("/events/recommendations").header(USER_HEADER, "invalid")).getStatus()).isEqualTo(400);
        assertThat(response(get("/events/recommendations").header(USER_HEADER, "0")).getStatus()).isEqualTo(400);

        verifyNoInteractions(users, events, collector, analyzer, eventStats);
    }

    @Test
    @DisplayName("An unknown user receives 404 before Analyzer is called")
    void rejectsUnknownUser() throws Exception {
        doThrow(new NotFoundException("User not found")).when(users).checkExistsByIdOrThrow(67L);

        assertThat(response(get("/events/recommendations").header(USER_HEADER, "67")).getStatus()).isEqualTo(404);

        verify(users, times(1)).checkExistsByIdOrThrow(67L);
        verifyNoInteractions(events, collector, analyzer, eventStats);
    }

    @Test
    @DisplayName("The private event listing keeps its user-scoped route")
    void listsEventsOwnedByUser() throws Exception {
        var page = PageRequest.of(0, 10);
        when(events.findByInitiatorId(67L, page)).thenReturn(List.of());

        var response = response(get("/users/67/events"));
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).isEqualTo("[]");

        verify(users, times(1)).checkExistsByIdOrThrow(67L);
        verify(events, times(1)).findByInitiatorId(67L, page);
        verifyNoInteractions(collector, analyzer, eventStats);
    }

    private MockHttpServletResponse response(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request).andReturn().getResponse();
    }

    private Event event(long id, EventState state) {
        return Event.builder().id(id).state(state).build();
    }

    private EventShortDto card(long id) {
        return EventShortDto.builder().id(id).build();
    }
}

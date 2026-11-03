package ru.practicum.stats.client;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.stats.client.util.TestDataUtils;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class StatsClientImplForFeignTest {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Mock
    private FeignStatsClient feign;

    private StatsClientImplForFeign client;
    private EasyRandom random;

    @BeforeEach
    void setUp() {
        client = new StatsClientImplForFeign(feign);
        random = TestDataUtils.createEasyRandomizer();
    }

    @AfterEach
    void verifyNoMoreFeignInteractions() {
        verifyNoMoreInteractions(feign);
    }

    @Test
    @DisplayName("hit delegates the original payload to Feign")
    void hitDelegatesPayload() {
        EndpointHitDto hit = random.nextObject(EndpointHitDto.class);

        client.hit(hit);

        verify(feign, times(1)).saveHit(hit);
        verifyNoMoreInteractions(feign);
    }

    @Test
    @DisplayName("hit swallows a Feign exception")
    void hitSwallowsFeignException() {
        EndpointHitDto hit = random.nextObject(EndpointHitDto.class);
        doThrow(feignException(503)).when(feign).saveHit(hit);

        assertThatCode(() -> client.hit(hit)).doesNotThrowAnyException();
        verify(feign, times(1)).saveHit(hit);
    }

    @Test
    @DisplayName("getStats formats dates and forwards URI filters and unique flag")
    void getStatsForwardsFormattedArguments() {
        LocalDateTime start = LocalDateTime.of(2026, 2, 3, 4, 5, 6);
        LocalDateTime end = LocalDateTime.of(2026, 5, 7, 8, 9, 10);
        List<String> uris = List.of("/events/15", "/events/21");
        List<ViewStatsDto> expected = List.of(random.nextObject(ViewStatsDto.class));
        when(feign.getStats(start.format(FORMATTER), end.format(FORMATTER), uris, true)).thenReturn(expected);

        List<ViewStatsDto> result = client.getStats(start, end, uris, true);

        assertThat(result).isSameAs(expected);
        verify(feign, times(1)).getStats(start.format(FORMATTER), end.format(FORMATTER), uris, true);
    }

    @Test
    @DisplayName("getStats treats a null unique flag as false")
    void getStatsNullUniqueIsFalse() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = start.plusDays(1);
        when(feign.getStats(start.format(FORMATTER), end.format(FORMATTER), null, false)).thenReturn(List.of());

        assertThat(client.getStats(start, end, null, null)).isEmpty();
        verify(feign, times(1)).getStats(start.format(FORMATTER), end.format(FORMATTER), null, false);
    }

    @ParameterizedTest(name = "status {0} maps to {1}")
    @MethodSource("errorResponses")
    @DisplayName("getStats maps Feign failures and keeps the original cause")
    void getStatsMapsFeignFailures(int status, Class<? extends RuntimeException> expectedException) {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime end = start.plusDays(1);
        FeignException failure = feignException(status);
        when(feign.getStats(start.format(FORMATTER), end.format(FORMATTER), List.of("/events/1"), false))
                .thenThrow(failure);

        assertThatThrownBy(() -> client.getStats(start, end, List.of("/events/1"), false))
                .isInstanceOf(expectedException)
                .hasCause(failure);
        verify(feign, times(1)).getStats(start.format(FORMATTER), end.format(FORMATTER), List.of("/events/1"), false);
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> errorResponses() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(400, IllegalArgumentException.class),
                org.junit.jupiter.params.provider.Arguments.of(503, IllegalStateException.class),
                org.junit.jupiter.params.provider.Arguments.of(-1, IllegalStateException.class)
        );
    }

    private static FeignException feignException(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "/stats", Map.of(), null, StandardCharsets.UTF_8);
        Response response = Response.builder()
                .status(status)
                .reason("failure")
                .headers(Map.of())
                .request(request)
                .build();
        return FeignException.errorStatus("GET /stats", response);
    }
}

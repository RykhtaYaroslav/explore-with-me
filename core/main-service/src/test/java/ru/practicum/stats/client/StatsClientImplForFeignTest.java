package ru.practicum.stats.client;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsClientImplForFeignTest {
    @Mock
    private FeignStatsClient feign;

    private StatsClientImplForFeign statsClient;

    @BeforeEach
    void setUp() {
        statsClient = new StatsClientImplForFeign(feign);
    }

    @Test
    @DisplayName("Передает hit Feign-клиенту")
    void shouldDelegateHitToFeignClient() {
        EndpointHitDto hit = EndpointHitDto.builder()
                .app("main-service")
                .uri("/events/1")
                .ip("192.0.2.1")
                .timestamp(LocalDateTime.of(2026, 10, 22, 12, 30))
                .build();

        statsClient.hit(hit);

        verify(feign).saveHit(hit);
    }

    @Test
    @DisplayName("Не пробрасывает ошибку Feign при отправке hit")
    void shouldSwallowFeignExceptionWhenSendingHit() {
        EndpointHitDto hit = EndpointHitDto.builder().uri("/events/1").build();
        FeignException exception = mock(FeignException.class);
        when(exception.getMessage()).thenReturn("Feign failure");
        doThrow(exception).when(feign).saveHit(hit);

        assertDoesNotThrow(() -> statsClient.hit(hit));
        verify(feign).saveHit(hit);
    }

    @Test
    @DisplayName("Форматирует даты и передает параметры запроса статистики")
    void shouldFormatDatesAndDelegateStatsRequest() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 22, 3, 4, 5);
        LocalDateTime end = LocalDateTime.of(2026, 10, 23, 13, 14, 15);
        List<String> uris = List.of("/events/1", "/events/2");
        List<ViewStatsDto> expected = List.of(ViewStatsDto.builder()
                .app("main-service")
                .uri("/events/1")
                .hits(3L)
                .build());
        when(feign.getStats("2026-10-22 03:04:05", "2026-10-23 13:14:15", uris, true))
                .thenReturn(expected);

        List<ViewStatsDto> actual = statsClient.getStats(start, end, uris, true);

        assertSame(expected, actual);
        verify(feign).getStats("2026-10-22 03:04:05", "2026-10-23 13:14:15", uris, true);
    }

    @Test
    @DisplayName("Считает null в параметре unique значением false")
    void shouldTreatNullUniqueAsFalse() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 22, 0, 0);
        LocalDateTime end = start.plusDays(1);
        when(feign.getStats(anyString(), anyString(), isNull(), anyBoolean())).thenReturn(List.of());

        statsClient.getStats(start, end, null, null);

        verify(feign).getStats("2026-10-22 00:00:00", "2026-10-23 00:00:00", null, false);
    }

    @Test
    @DisplayName("Преобразует ответ 4xx в IllegalArgumentException и сохраняет причину")
    void shouldTranslateClientError() {
        FeignException exception = feignException(400);
        when(feign.getStats(anyString(), anyString(), any(), anyBoolean())).thenThrow(exception);

        IllegalArgumentException actual = assertThrows(IllegalArgumentException.class,
                () -> statsClient.getStats(LocalDateTime.now(), LocalDateTime.now(), null, false));

        assertSame(exception, actual.getCause());
    }

    @Test
    @DisplayName("Преобразует ответ 5xx в IllegalStateException и сохраняет причину")
    void shouldTranslateServerError() {
        assertUnavailableErrorIsTranslated(503);
    }

    @Test
    @DisplayName("Преобразует сетевую ошибку Feign в IllegalStateException")
    void shouldTranslateNetworkError() {
        assertUnavailableErrorIsTranslated(-1);
    }

    private void assertUnavailableErrorIsTranslated(int status) {
        FeignException exception = feignException(status);
        when(feign.getStats(anyString(), anyString(), any(), anyBoolean())).thenThrow(exception);

        IllegalStateException actual = assertThrows(IllegalStateException.class,
                () -> statsClient.getStats(LocalDateTime.now(), LocalDateTime.now(), null, false));

        assertSame(exception, actual.getCause());
    }

    private FeignException feignException(int status) {
        FeignException exception = mock(FeignException.class);
        when(exception.status()).thenReturn(status);
        when(exception.getMessage()).thenReturn("Feign failure");
        return exception;
    }
}

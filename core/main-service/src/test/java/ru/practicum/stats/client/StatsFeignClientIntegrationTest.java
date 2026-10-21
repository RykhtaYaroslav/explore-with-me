package ru.practicum.stats.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.main.MainApplication;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = MainApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class StatsFeignClientIntegrationTest {

    @Autowired
    private StatsClient statsClient;

    @Test
    @DisplayName("Отправляет hit и получает его статистику через Feign и Eureka")
    void shouldSendHitAndGetStatsFromStatsServerThroughFeign() {
        assertInstanceOf(StatsClientImplForFeign.class, statsClient);

        LocalDateTime timestamp = LocalDateTime.now().withNano(0);
        String uri = "/integration/" + UUID.randomUUID();
        statsClient.hit(EndpointHitDto.builder()
                .app("main-service")
                .uri(uri)
                .ip("192.0.2.10")
                .timestamp(timestamp)
                .build());

        List<ViewStatsDto> stats = statsClient.getStats(
                timestamp.minusMinutes(1),
                timestamp.plusMinutes(1),
                List.of(uri),
                false
        );

        assertNotNull(stats);
        assertEquals(1, stats.size());
        assertEquals(uri, stats.getFirst().getUri());
        assertEquals(1L, stats.getFirst().getHits());
    }

    @Test
    @DisplayName("Преобразует ответ 4xx Stats Server в ошибку некорректного запроса")
    void shouldTranslateStatsServerBadRequest() {
        LocalDateTime end = LocalDateTime.now();

        assertThrows(IllegalArgumentException.class, () -> statsClient.getStats(
                end,
                end.minusSeconds(1),
                null,
                false
        ));
    }
}

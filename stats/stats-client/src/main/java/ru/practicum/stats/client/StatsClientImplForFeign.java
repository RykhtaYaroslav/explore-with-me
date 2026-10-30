package ru.practicum.stats.client;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "stats.client",
        name = "type",
        havingValue = "feign",
        matchIfMissing = true
)
public class StatsClientImplForFeign implements StatsClient {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FeignStatsClient feign;

    @Override
    public void hit(EndpointHitDto hitDto) {
        try {
            feign.saveHit(hitDto);
        } catch (FeignException exception) {
            log.error("Failed to send statistics for URI: {}. Error: {}",
                    hitDto.getUri(), exception.getMessage());
        }
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        try {

            return feign.getStats(start.format(FORMATTER), end.format(FORMATTER), uris, Boolean.TRUE.equals(unique));

        } catch (FeignException exception) {
            int status = exception.status();
            if (status >= 400 && status < 500) {
                log.error("Statistics request failed (4xx): {}", status, exception);
                throw new IllegalArgumentException(
                        "Invalid date range for statistics collection", exception);
            }

            log.error("Statistics server is unavailable. Error: {}", exception.getMessage(), exception);
            throw new IllegalStateException("The statistics server is temporarily unavailable", exception);
        }
    }
}

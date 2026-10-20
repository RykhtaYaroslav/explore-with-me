package ru.practicum.stats.service;

import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatService {
    void saveHit(EndpointHitDto hit);

    List<ViewStatsDto> getStats(LocalDateTime startDate,
                                LocalDateTime endDate,
                                List<String> uris,
                                boolean unique);
}

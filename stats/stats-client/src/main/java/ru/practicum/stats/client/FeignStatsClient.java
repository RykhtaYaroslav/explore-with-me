package ru.practicum.stats.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.util.List;

@FeignClient(name = "stats-server")
public interface FeignStatsClient {
    @PostMapping("/hit")
    void saveHit(@RequestBody EndpointHitDto hit);

    @GetMapping("/stats")
    List<ViewStatsDto> getStats(@RequestParam("start") String start,
                                @RequestParam("end") String end,
                                @RequestParam(value = "uris", required = false) List<String> uris,
                                @RequestParam(value = "unique", defaultValue = "false") boolean unique);
}

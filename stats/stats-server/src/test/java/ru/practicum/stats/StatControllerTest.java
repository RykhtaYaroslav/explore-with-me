package ru.practicum.stats;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import ru.practicum.stats.controller.StatController;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.service.StatService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.mockito.Mockito.when;

@WebMvcTest(StatController.class)
@ContextConfiguration(classes = StatsServerApplication.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class StatControllerTest {
    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    private final LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);
    private final LocalDateTime end = LocalDateTime.of(2026, 6, 20, 0, 0);

    private final String startStr = start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    private final String endStr = end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    @MockBean
    private StatService statService;

    @Test
    void saveHitTest() throws Exception {
        EndpointHitDto hit = new EndpointHitDto("app", "uri", "192.0.0.1", LocalDateTime.now());

        mockMvc.perform(MockMvcRequestBuilders.post("/hit")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(hit)))
                .andExpect(MockMvcResultMatchers.status().isCreated());
    }

    @Test
    void getStatsWithUris() throws Exception {
        List<String> uris = List.of("/users", "/events");

        List<ViewStatsDto> mockData = List.of(
                new ViewStatsDto("app1", "/users", 2L),
                new ViewStatsDto("app2", "/event", 1L)
        );

        when(statService.getStats(start, end, uris, false))
                .thenReturn(mockData);

        mockMvc.perform(MockMvcRequestBuilders.get("/stats")
                        .param("start", startStr)
                        .param("end", endStr)
                        .param("uris", uris.toArray(new String[0]))
                        .param("unique", "false"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].app").value("app1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].uri").value("/users"))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].hits").value(2));
    }

    @Test
    void getEmptyStatsWithoutUris() throws Exception {
        when(statService.getStats(start, end, null, true))
                .thenReturn(List.of());

        mockMvc.perform(MockMvcRequestBuilders.get("/stats")
                        .param("start", startStr)
                        .param("end", endStr)
                        .param("unique", "true"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("[]"));
    }

    @Test
    void getStatsWhereStartAfterEnd() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/stats")
                        .param("start", endStr)
                        .param("end", startStr)
                        .param("unique", "true"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }
}

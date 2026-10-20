package ru.practicum.stats.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

public interface StatRepository extends JpaRepository<EndpointHit, Long> {
    @Query("""
             select new ru.practicum.stats.dto.ViewStatsDto(h.app, h.uri, COUNT(distinct h.ip))
             from EndpointHit h where h.timestamp between :start and :end
             and (:uris is null or h.uri in :uris)
             group by h.app, h.uri
             order by COUNT(distinct h.ip) desc
            """)
    List<ViewStatsDto> getStatUnique(@Param("start") LocalDateTime startDate,
                                     @Param("end") LocalDateTime endDate,
                                     @Param("uris") List<String> uris);

    @Query("""
            select new ru.practicum.stats.dto.ViewStatsDto(h.app, h.uri, COUNT(h.ip))
            from EndpointHit h where h.timestamp between :start and :end
            and (:uris is null or h.uri in :uris)
            group by h.app, h.uri
            order by COUNT(h.ip) desc
            """)
    List<ViewStatsDto> getStat(@Param("start") LocalDateTime startDate,
                               @Param("end") LocalDateTime endDate,
                               @Param("uris") List<String> uris);
}

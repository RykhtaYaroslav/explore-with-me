package ru.practicum.stats.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;
import ru.practicum.stats.mapper.EndpointHitMapper;
import ru.practicum.stats.model.EndpointHit;
import ru.practicum.stats.repository.StatRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class StatServiceImpl implements StatService {
    private final StatRepository statRepository;
    private final EndpointHitMapper mapper;

    @Override
    @Transactional
    public void saveHit(EndpointHitDto hit) {
        EndpointHit entity = mapper.toEntity(hit);
        statRepository.save(entity);
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime startDate, LocalDateTime endDate, List<String> uris, boolean unique) {
        return unique ?
                statRepository.getStatUnique(startDate, endDate, uris) : statRepository.getStat(startDate, endDate, uris);
    }
}

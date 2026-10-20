package ru.practicum.stats.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.model.EndpointHit;

@Mapper(componentModel = "spring")
@SuppressWarnings("unused")
public interface EndpointHitMapper {
    @Mapping(target = "id", ignore = true)
    EndpointHit toEntity(EndpointHitDto dto);

    EndpointHitDto toDto(EndpointHit entity);
}

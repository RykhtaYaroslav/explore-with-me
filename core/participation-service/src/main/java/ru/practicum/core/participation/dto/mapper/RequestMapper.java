package ru.practicum.core.participation.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.core.participation.model.ParticipationRequest;
import ru.practicum.main.request.dto.ParticipationRequestDto;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    @Mapping(target = "eventId", source = "requestEntity.eventId")
    @Mapping(target = "requesterId", source = "requestEntity.requesterId")
    ParticipationRequestDto toDtoOut(ParticipationRequest requestEntity);
}

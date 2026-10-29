package ru.practicum.core.participation.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.core.participation.model.ParticipationRequest;
import ru.practicum.main.request.dto.ParticipationRequestDto;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    @Mapping(target = "eventId", source = "requestEntity.event.id")
    @Mapping(target = "requesterId", source = "requestEntity.requester.id")
    ParticipationRequestDto toDtoOut(ParticipationRequest requestEntity);
}

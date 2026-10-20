package ru.practicum.main.request.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.main.request.dto.ParticipationRequestDto;
import ru.practicum.main.request.model.ParticipationRequest;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    @Mapping(target = "eventId", source = "requestEntity.event.id")
    @Mapping(target = "requesterId", source = "requestEntity.requester.id")
    ParticipationRequestDto toDtoOut(ParticipationRequest requestEntity);
}

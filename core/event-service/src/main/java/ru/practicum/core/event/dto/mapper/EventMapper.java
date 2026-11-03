package ru.practicum.core.event.dto.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.practicum.core.category.model.Category;
import ru.practicum.core.event.dto.LocationDto;
import ru.practicum.core.event.dto.request.NewEventDto;
import ru.practicum.core.event.dto.request.UpdateEventAdminRequest;
import ru.practicum.core.event.dto.request.UpdateEventUserRequest;
import ru.practicum.core.event.dto.response.EventFullDto;
import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.core.event.model.Event;
import ru.practicum.core.event.model.Location;
import ru.practicum.core.user.dto.UserShortDto;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring", imports = LocalDateTime.class)
public interface EventMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "createdOn", expression = "java(LocalDateTime.now())")
    @Mapping(target = "state", constant = "PENDING")
    @Mapping(target = "location", source = "newEventDto.location")
    @Mapping(target = "confirmedRequests", ignore = true)
    Event toEntity(NewEventDto newEventDto, Long initiatorId, Category category);

    Location toLocation(LocationDto locationDto);

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "initiator", source = "initiator")
    @Mapping(target = "confirmedRequests", source = "confirmedRequests")
    @Mapping(target = "rating", source = "rating")
    EventFullDto toFullDto(Event event, UserShortDto initiator, Long views, Long confirmedRequests, Double rating);

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "initiator", source = "initiator")
    @Mapping(target = "confirmedRequests", source = "confirmedRequests")
    @Mapping(target = "rating", source = "rating")
    EventShortDto toShortDto(Event event, UserShortDto initiator, Long views, Long confirmedRequests, Double rating);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    void updateEventFromUserDto(UpdateEventUserRequest request, @MappingTarget Event event);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "initiatorId", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "location", source = "request.location")
    void updateEventFromAdminDto(UpdateEventAdminRequest request, @MappingTarget Event event);
}

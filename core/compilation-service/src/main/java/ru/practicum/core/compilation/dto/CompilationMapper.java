package ru.practicum.core.compilation.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.core.compilation.model.Compilation;
import ru.practicum.core.event.model.Event;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CompilationMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", source = "eventIds")
    Compilation toCompilation(NewCompilationDto dto, List<Long> eventIds);

    @Mapping(target = "events", source = "events")
    CompilationDto toDto(Compilation compilation, List<Event> events);
}

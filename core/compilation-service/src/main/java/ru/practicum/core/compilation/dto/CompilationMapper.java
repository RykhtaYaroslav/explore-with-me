package ru.practicum.core.compilation.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.core.compilation.model.Compilation;
import ru.practicum.core.event.model.Event;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CompilationMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", source = "events")
    Compilation toCompilation(NewCompilationDto dto, List<Event> events);

    CompilationDto toDto(Compilation compilation);
}

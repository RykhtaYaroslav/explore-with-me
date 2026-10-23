package ru.practicum.main.compilation.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.main.compilation.model.Compilation;
import ru.practicum.main.event.model.Event;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CompilationMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", source = "events")
    Compilation toCompilation(NewCompilationDto dto, List<Event> events);

    CompilationDto toDto(Compilation compilation);
}

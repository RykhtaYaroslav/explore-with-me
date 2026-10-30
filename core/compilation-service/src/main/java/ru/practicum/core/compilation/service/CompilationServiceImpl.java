package ru.practicum.core.compilation.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.core.common.exception.ConflictException;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.compilation.dto.CompilationDto;
import ru.practicum.core.compilation.dto.CompilationMapper;
import ru.practicum.core.compilation.dto.NewCompilationDto;
import ru.practicum.core.compilation.dto.UpdateCompilationRequest;
import ru.practicum.core.compilation.interservice.EventInterservice;
import ru.practicum.core.compilation.model.Compilation;
import ru.practicum.core.compilation.repository.CompilationRepository;
import ru.practicum.core.event.dto.response.EventShortDto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
@Transactional
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventInterservice eventInterservice;
    private final CompilationMapper mapper;

    @Override
    public CompilationDto createCompilation(NewCompilationDto compilationDto) {
        if (compilationRepository.existsByTitle(compilationDto.getTitle())) {
            throw new ConflictException(String.format("A compilation with title '%s' already exists", compilationDto.getTitle()));
        }

        List<EventShortDto> events = findEvents(compilationDto.getEvents());
        List<Long> eventIds = events.stream().map(EventShortDto::getId).toList();
        Compilation compilation = mapper.toCompilation(compilationDto, eventIds);

        return mapper.toDto(compilationRepository.save(compilation), events);
    }

    @Override
    public void deleteCompilation(long compId) {
        checkAndReturnCompilation(compId);

        compilationRepository.deleteById(compId);
    }

    @Override
    public CompilationDto updateCompilation(long compId, UpdateCompilationRequest update) {
        Compilation compilation = checkAndReturnCompilation(compId);
        if (update.getTitle() != null
                && !update.getTitle().equals(compilation.getTitle())
                && compilationRepository.existsByTitle(update.getTitle())) {
            throw new ConflictException(String.format("A compilation with title '%s' already exists", update.getTitle()));
        }
        if (update.getTitle() != null) {
            compilation.setTitle(update.getTitle());
        }
        if (update.getEvents() != null) {
            List<EventShortDto> events = findEvents(update.getEvents());
            compilation.setEvents(events.stream().map(EventShortDto::getId).toList());
        }
        if (update.getPinned() != null) {
            compilation.setPinned(update.getPinned());
        }
        return toDto(compilationRepository.save(compilation));
    }

    private List<EventShortDto> findEvents(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }
        return eventInterservice.findByIds(eventIds);
    }

    private CompilationDto toDto(Compilation compilation) {
        List<EventShortDto> events = findEvents(compilation.getEvents());
        return mapper.toDto(compilation, events);
    }

    private Compilation checkAndReturnCompilation(long compId) {
        return compilationRepository.findById(compId).orElseThrow(() ->
                new NotFoundException(String.format("Compilation with id = %d was not found", compId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {

        Pageable pageable = PageRequest.of(from / size, size);

        List<Compilation> compilations;

        if (pinned == null) {
            compilations = compilationRepository.findAll(pageable).getContent();
        } else {

            compilations = compilationRepository.findAllByPinned(pinned, pageable);
        }

        Map<Long, List<Long>> eventIdsByCompilationId = new LinkedHashMap<>();
        compilations.forEach(compilation ->
                eventIdsByCompilationId.put(compilation.getId(), compilation.getEvents()));

        List<Long> eventIds = eventIdsByCompilationId.values().stream()
                .flatMap(List::stream)
                .distinct()
                .toList();

        List<EventShortDto> eventDtos = eventIds.isEmpty()
                ? Collections.emptyList()
                : eventInterservice.findByIds(eventIds);
        Map<Long, EventShortDto> eventDtosById = eventDtos.stream()
                .collect(Collectors.toMap(EventShortDto::getId, eventDto -> eventDto));

        return compilations.stream()
                .map(compilation -> {
                    List<EventShortDto> compilationEvents = eventIdsByCompilationId.get(compilation.getId()).stream()
                            .map(eventDtosById::get)
                            .filter(Objects::nonNull)
                            .toList();
                    return mapper.toDto(compilation, compilationEvents);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto getCompilationById(Long compId) {

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException(String.format("Compilation with id = %d was not found", compId)));

        return toDto(compilation);
    }
}

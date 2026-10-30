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
import ru.practicum.core.compilation.model.Compilation;
import ru.practicum.core.compilation.repository.CompilationRepository;
import ru.practicum.core.event.model.Event;
import ru.practicum.core.event.repository.EventRepository;

import java.util.Collections;
import java.util.List;

@AllArgsConstructor
@Service
@Transactional
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper mapper;

    @Override
    public CompilationDto createCompilation(NewCompilationDto compilationDto) {
        if (compilationRepository.existsByTitle(compilationDto.getTitle())) {
            throw new ConflictException(String.format("A compilation with title '%s' already exists", compilationDto.getTitle()));
        }

        List<Event> events = findEvents(compilationDto.getEvents());
        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Compilation comp = mapper.toCompilation(compilationDto, eventIds);

        return mapper.toDto(compilationRepository.save(comp), events);
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
            List<Event> events = findEvents(update.getEvents());
            compilation.setEvents(events.stream().map(Event::getId).toList());
        }
        if (update.getPinned() != null) {
            compilation.setPinned(update.getPinned());
        }
        return toDto(compilationRepository.save(compilation));
    }

    private List<Event> findEvents(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyList();
        }
        return eventRepository.findAllById(eventIds);
    }

    private CompilationDto toDto(Compilation compilation) {
        List<Event> events = findEvents(compilation.getEvents());
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

        return compilations.stream()
                .map(this::toDto)
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

package ru.practicum.main.compilation.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.practicum.main.compilation.dto.CompilationDto;
import ru.practicum.main.compilation.dto.CompilationMapper;
import ru.practicum.main.compilation.dto.NewCompilationDto;
import ru.practicum.main.compilation.dto.UpdateCompilationRequest;
import ru.practicum.main.compilation.model.Compilation;
import ru.practicum.main.compilation.repository.CompilationRepository;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;

import java.util.ArrayList;
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
            throw new ConflictException("Подборка с заголовком = " + compilationDto.getTitle() + " уже существует");
        }

        List<Event> events = Collections.emptyList();
        if (!CollectionUtils.isEmpty(compilationDto.getEvents())) {
            events = eventRepository.findAllById(compilationDto.getEvents());
        }

        Compilation comp = mapper.toCompilation(compilationDto, events);

        // На всякий случай гарантируем, что список events не null (защита от особенностей Lombok Builder)
        if (comp.getEvents() == null) {
            comp.setEvents(new ArrayList<>());
        }

        return mapper.toDto(compilationRepository.save(comp));
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
            throw new ConflictException("Подборка с заголовком = " + update.getTitle() + " уже существует");
        }
        if (update.getTitle() != null) {
            compilation.setTitle(update.getTitle());
        }
        if (update.getEvents() != null) {
            List<Event> events = eventRepository.findAllById(update.getEvents());
            compilation.setEvents(events);
        }
        if (update.getPinned() != null) {
            compilation.setPinned(update.getPinned());
        }
        return mapper.toDto(compilationRepository.save(compilation));
    }

    private Compilation checkAndReturnCompilation(long compId) {
        return compilationRepository.findById(compId).orElseThrow(() ->
                new NotFoundException("Подборка с id = " + compId + " не найдена!"));
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
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto getCompilationById(Long compId) {

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));

        return mapper.toDto(compilation);
    }
}

package ru.practicum.core.compilation.service;

import ru.practicum.core.compilation.dto.CompilationDto;
import ru.practicum.core.compilation.dto.NewCompilationDto;
import ru.practicum.core.compilation.dto.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {
    CompilationDto createCompilation(NewCompilationDto compilation);

    void deleteCompilation(long compId);

    CompilationDto updateCompilation(long compId, UpdateCompilationRequest update);

    List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size);

    CompilationDto getCompilationById(Long compId);
}

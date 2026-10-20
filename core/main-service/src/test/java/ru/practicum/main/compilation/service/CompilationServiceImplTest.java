package ru.practicum.main.compilation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.main.compilation.dto.CompilationDto;
import ru.practicum.main.compilation.dto.CompilationMapper;
import ru.practicum.main.compilation.model.Compilation;
import ru.practicum.main.compilation.repository.CompilationRepository;
import ru.practicum.main.exception.NotFoundException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompilationServiceImplTest {

    @Mock
    private CompilationRepository compilationRepository;

    @Mock
    private CompilationMapper mapper;

    @InjectMocks
    private CompilationServiceImpl compilationService;

    private Compilation compilation;
    private CompilationDto compilationDto;

    @BeforeEach
    void setUp() {
        compilation = new Compilation();
        compilation.setId(1L);
        compilation.setTitle("Test compilation");
        compilation.setPinned(true);

        compilationDto = new CompilationDto();
        compilationDto.setId(1L);
        compilationDto.setTitle("Test compilation");
        compilationDto.setPinned(true);
    }

    @Test
    void getCompilations_whenPinnedIsNull_shouldReturnAllCompilations() {
        Integer from = 0;
        Integer size = 10;
        Pageable pageable = PageRequest.of(from / size, size);
        Page<Compilation> page = new PageImpl<>(List.of(compilation));

        when(compilationRepository.findAll(pageable)).thenReturn(page);
        when(mapper.toDto(compilation)).thenReturn(compilationDto);

        List<CompilationDto> result = compilationService.getCompilations(null, from, size);

        assertEquals(1, result.size());
        assertEquals(compilationDto, result.get(0));
        verify(compilationRepository, times(1)).findAll(pageable);
        verify(compilationRepository, times(0)).findAllByPinned(any(), any());
    }

    @Test
    void getCompilations_whenPinnedIsProvided_shouldReturnFilteredCompilations() {
        Integer from = 0;
        Integer size = 10;
        Boolean pinned = true;
        Pageable pageable = PageRequest.of(from / size, size);

        when(compilationRepository.findAllByPinned(pinned, pageable))
                .thenReturn(List.of(compilation));
        when(mapper.toDto(compilation)).thenReturn(compilationDto);

        List<CompilationDto> result = compilationService.getCompilations(pinned, from, size);

        assertEquals(1, result.size());
        assertEquals(compilationDto, result.get(0));
        verify(compilationRepository, times(1)).findAllByPinned(pinned, pageable);
        verify(compilationRepository, times(0)).findAll(any(Pageable.class));
    }

    @Test
    void getCompilationById_whenCompilationExists_shouldReturnDto() {
        Long compId = 1L;

        when(compilationRepository.findById(compId)).thenReturn(Optional.of(compilation));
        when(mapper.toDto(compilation)).thenReturn(compilationDto);

        CompilationDto result = compilationService.getCompilationById(compId);

        assertEquals(compilationDto, result);
        verify(compilationRepository, times(1)).findById(compId);
    }

    @Test
    void getCompilationById_whenCompilationNotFound_shouldThrowNotFoundException() {
        Long compId = 999L;

        when(compilationRepository.findById(compId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> compilationService.getCompilationById(compId)
        );

        assertEquals("Подборка с id=" + compId + " не найдена", exception.getMessage());
        verify(compilationRepository, times(1)).findById(compId);
    }
}
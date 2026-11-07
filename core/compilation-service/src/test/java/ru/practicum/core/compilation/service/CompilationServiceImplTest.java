package ru.practicum.core.compilation.service;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.compilation.dto.CompilationDto;
import ru.practicum.core.compilation.dto.CompilationMapper;
import ru.practicum.core.compilation.interservice.EventInterservice;
import ru.practicum.core.compilation.model.Compilation;
import ru.practicum.core.compilation.repository.CompilationRepository;
import ru.practicum.core.compilation.util.TestDataUtils;
import ru.practicum.core.event.dto.response.EventShortDto;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompilationServiceImplTest {
    @Mock
    private CompilationRepository repository;
    @Mock
    private EventInterservice eventInterservice;

    private CompilationServiceImpl service;
    private CompilationMapper mapper;
    private EasyRandom random;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(CompilationMapper.class);
        service = new CompilationServiceImpl(repository, eventInterservice, mapper);
        random = TestDataUtils.createEasyRandomizer();
    }

    @AfterEach
    void verifyNoMoreMockInteractions() {
        verifyNoMoreInteractions(repository, eventInterservice);
    }

    @ParameterizedTest(name = "pinned filter is {0}")
    @NullSource
    @ValueSource(booleans = {true, false})
    @DisplayName("getCompilations chooses the correct repository query and maps events")
    void getCompilations(Boolean pinned) {
        Compilation compilation = compilation(5L, true, List.of(21L));
        PageRequest pageable = PageRequest.of(2, 10);
        if (pinned == null) {
            when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(compilation), pageable, 1));
        } else {
            when(repository.findAllByPinned(pinned, pageable)).thenReturn(List.of(compilation));
        }
        EventShortDto event = event(21L);
        when(eventInterservice.findByIds(List.of(21L))).thenReturn(List.of(event));

        List<CompilationDto> result = service.getCompilations(pinned, 20, 10);

        assertThat(result).singleElement().satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo(5L);
            assertThat(dto.getPinned()).isTrue();
            assertThat(dto.getEvents()).containsExactly(event);
        });
        if (pinned == null) {
            verify(repository, times(1)).findAll(pageable);
        } else {
            verify(repository, times(1)).findAllByPinned(pinned, pageable);
        }
        verify(eventInterservice, times(1)).findByIds(List.of(21L));
    }

    @Test
    @DisplayName("getCompilationById maps an existing compilation and its events")
    void getCompilationById() {
        Compilation compilation = compilation(7L, false, List.of(31L));
        EventShortDto event = event(31L);
        event.setTotalInteractionWeight(2.8);
        when(repository.findById(7L)).thenReturn(Optional.of(compilation));
        when(eventInterservice.findByIds(List.of(31L))).thenReturn(List.of(event));

        CompilationDto result = service.getCompilationById(7L);

        assertThat(result.getId()).isEqualTo(7L);
        assertThat(result.getEvents()).containsExactly(event);
        assertThat(result.getEvents()).extracting(EventShortDto::getTotalInteractionWeight).containsExactly(2.8);
        verify(repository, times(1)).findById(7L);
        verify(eventInterservice, times(1)).findByIds(List.of(31L));
    }

    @Test
    @DisplayName("getCompilationById reports a missing compilation with its ID")
    void getCompilationByIdMissing() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCompilationById(404L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Compilation with id = 404 was not found");
        verifyNoInteractions(eventInterservice);
        verify(repository, times(1)).findById(404L);
    }

    private Compilation compilation(Long id, Boolean pinned, List<Long> events) {
        Compilation compilation = random.nextObject(Compilation.class);
        compilation.setId(id);
        compilation.setPinned(pinned);
        compilation.setEvents(events);
        return compilation;
    }

    private EventShortDto event(Long id) {
        EventShortDto event = new EventShortDto();
        event.setId(id);
        event.setTitle(random.nextObject(String.class));
        return event;
    }
}

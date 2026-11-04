package ru.practicum.core.category.service;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import ru.practicum.core.category.dto.CategoryMapper;
import ru.practicum.core.category.model.Category;
import ru.practicum.core.category.repository.CategoryRepository;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.event.category.dto.CategoryDto;
import ru.practicum.core.event.repository.EventRepository;
import ru.practicum.core.event.util.TestDataUtils;

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
class CategoryServiceImplTest {
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private EventRepository eventRepository;

    private CategoryServiceImpl service;
    private EasyRandom random;

    @BeforeEach
    void setUp() {
        service = new CategoryServiceImpl(categoryRepository, eventRepository, Mappers.getMapper(CategoryMapper.class));
        random = TestDataUtils.createEventEasyRandomizer();
    }

    @AfterEach
    void verifyNoMoreMockInteractions() {
        verifyNoMoreInteractions(categoryRepository, eventRepository);
    }

    @Test
    @DisplayName("getCategoryById returns a mapped existing category")
    void getExistingCategory() {
        Category category = category(12L);
        when(categoryRepository.findById(12L)).thenReturn(Optional.of(category));

        CategoryDto result = service.getCategoryById(12L);

        assertThat(result.getId()).isEqualTo(12L);
        assertThat(result.getName()).isEqualTo(category.getName());
        verify(categoryRepository, times(1)).findById(12L);
        verifyNoInteractions(eventRepository);
    }

    @Test
    @DisplayName("getCategoryById throws NotFoundException for a missing category")
    void getMissingCategory() {
        when(categoryRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCategoryById(404L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Category with id = 404 was not found");
        verify(categoryRepository, times(1)).findById(404L);
        verifyNoInteractions(eventRepository);
    }

    @Test
    @DisplayName("getCategories maps the requested page")
    void getCategoriesPage() {
        List<Category> categories = List.of(category(1L), category(2L));
        when(categoryRepository.findAll(PageRequest.of(1, 2))).thenReturn(new org.springframework.data.domain.PageImpl<>(categories));

        List<CategoryDto> result = service.getCategories(PageRequest.of(1, 2));

        assertThat(result).extracting(CategoryDto::getId).containsExactly(1L, 2L);
        assertThat(result).extracting(CategoryDto::getName)
                .containsExactly(categories.get(0).getName(), categories.get(1).getName());
        verify(categoryRepository, times(1)).findAll(PageRequest.of(1, 2));
        verifyNoInteractions(eventRepository);
    }

    private Category category(Long id) {
        Category category = random.nextObject(Category.class);
        category.setId(id);
        return category;
    }
}

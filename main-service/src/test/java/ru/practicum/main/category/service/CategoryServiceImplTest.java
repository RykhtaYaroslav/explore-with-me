package ru.practicum.main.category.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.main.category.dto.CategoryDto;
import ru.practicum.main.category.dto.CategoryMapper;
import ru.practicum.main.category.model.Category;
import ru.practicum.main.category.repository.CategoryRepository;
import ru.practicum.main.exception.NotFoundException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Spy
    @SuppressWarnings("all")
    private CategoryMapper categoryMapper = Mappers.getMapper(CategoryMapper.class);

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void getCategoryById_whenExist_shouldReturnCategoryDto() {

        Category category = new Category();
        category.setId(1L);
        category.setName("Выставки");

        Mockito.when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        CategoryDto result = categoryService.getCategoryById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Выставки", result.getName());

        Mockito.verify(categoryRepository, Mockito.times(1)).findById(1L);
    }

    @Test
    void getCategoryById_whenNotExist_shouldThrowNotFoundException() {

        Mockito.when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            categoryService.getCategoryById(99L);
        });

        assertEquals("Категория с id = 99 не найдена!", exception.getMessage());

        Mockito.verify(categoryRepository, Mockito.times(1)).findById(99L);
    }

    @Test
    void getCategories_shouldReturnListOfCategoryDtos() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Выставки");

        Pageable pageable = PageRequest.of(0, 10);

        PageImpl<Category> page = new PageImpl<>(List.of(category));

        Mockito.when(categoryRepository.findAll(pageable)).thenReturn(page);

        List<CategoryDto> result = categoryService.getCategories(pageable);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Выставки", result.get(0).getName());

        Mockito.verify(categoryRepository, Mockito.times(1)).findAll(pageable);
    }
}

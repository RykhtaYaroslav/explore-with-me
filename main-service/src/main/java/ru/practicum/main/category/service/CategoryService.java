package ru.practicum.main.category.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.main.category.dto.CategoryDto;
import ru.practicum.main.category.dto.NewCategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto createCategory(NewCategoryDto category);

    void deleteCategory(Long catId);

    CategoryDto updateCategory(CategoryDto category, long catId);

    //public category
    List<CategoryDto> getCategories(Pageable pageable);

    CategoryDto getCategoryById(Long catId);
}

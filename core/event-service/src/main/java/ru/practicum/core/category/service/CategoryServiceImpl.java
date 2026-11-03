package ru.practicum.core.category.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.core.category.dto.CategoryMapper;
import ru.practicum.core.category.model.Category;
import ru.practicum.core.category.repository.CategoryRepository;
import ru.practicum.core.common.exception.ConflictException;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.event.category.dto.CategoryDto;
import ru.practicum.core.event.category.dto.NewCategoryDto;
import ru.practicum.core.event.repository.EventRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final CategoryMapper mapper;

    @Override
    public CategoryDto createCategory(NewCategoryDto category) {
        if (categoryRepository.findByName(category.getName()) != null) {
            throw new ConflictException("This category already exists");
        }
        Category ct = mapper.toCategory(category);
        return mapper.toCategoryDtoOut(categoryRepository.save(ct));
    }

    @Override
    public void deleteCategory(Long catId) {
        checkAndReturnCategory(catId);
        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("This category is still used by an event");
        }
        categoryRepository.deleteById(catId);
    }

    @Override
    public CategoryDto updateCategory(CategoryDto category, long catId) {
        Category ct = checkAndReturnCategory(catId);
        if (categoryRepository.findByName(category.getName()) != null && !ct.getName().equals(category.getName())) {
            throw new ConflictException("This category already exists");
        }
        ct.setName(category.getName());
        return mapper.toCategoryDtoOut(categoryRepository.save(ct));
    }

    private Category checkAndReturnCategory(long catId) {
        return categoryRepository.findById(catId).orElseThrow(() ->
                new NotFoundException(String.format("Category with id = %d was not found", catId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable).stream()
                .map(mapper::toCategoryDtoOut)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException(String.format("Category with id = %d was not found", catId)));
        return mapper.toCategoryDtoOut(category);
    }
}

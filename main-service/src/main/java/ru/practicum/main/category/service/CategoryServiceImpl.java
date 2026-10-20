package ru.practicum.main.category.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.category.dto.CategoryDto;
import ru.practicum.main.category.dto.CategoryMapper;
import ru.practicum.main.category.dto.NewCategoryDto;
import ru.practicum.main.category.model.Category;
import ru.practicum.main.category.repository.CategoryRepository;
import ru.practicum.main.event.repository.EventRepository;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;

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
            throw new ConflictException("Данная категория уже существует!");
        }
        Category ct = mapper.toCategory(category);
        return mapper.toCategoryDtoOut(categoryRepository.save(ct));
    }

    @Override
    public void deleteCategory(Long catId) {
        checkAndReturnCategory(catId);
        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("Категория еще используется каким-то событием!");
        }
        categoryRepository.deleteById(catId);
    }

    @Override
    public CategoryDto updateCategory(CategoryDto category, long catId) {
        Category ct = checkAndReturnCategory(catId);
        if (categoryRepository.findByName(category.getName()) != null && !ct.getName().equals(category.getName())) {
            throw new ConflictException("Данная категория уже существует!");
        }
        ct.setName(category.getName());
        return mapper.toCategoryDtoOut(categoryRepository.save(ct));
    }

    private Category checkAndReturnCategory(long catId) {
        return categoryRepository.findById(catId).orElseThrow(() ->
                new NotFoundException("Категория с id = " + catId + " не найдена!"));
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
                .orElseThrow(() -> new NotFoundException("Категория с id = " + catId + " не найдена!"));
        return mapper.toCategoryDtoOut(category);
    }
}

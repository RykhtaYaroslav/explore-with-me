package ru.practicum.core.category.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.core.category.model.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    @Mapping(target = "id", ignore = true)
    Category toCategory(NewCategoryDto dto);

    CategoryDto toCategoryDtoOut(Category entity);
}

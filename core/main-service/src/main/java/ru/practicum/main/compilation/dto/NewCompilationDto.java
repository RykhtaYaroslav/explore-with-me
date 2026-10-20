package ru.practicum.main.compilation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewCompilationDto {
    private List<Long> events;

    @Builder.Default
    private Boolean pinned = false;

    @NotBlank(message = "Заголовок не может быть пустым")
    @Size(min = 1, max = 50, message = "Заголовок должен быть от {min} до {max} символов")
    private String title;
}
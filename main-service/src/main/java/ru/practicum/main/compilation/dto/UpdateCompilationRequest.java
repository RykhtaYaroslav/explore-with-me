package ru.practicum.main.compilation.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCompilationRequest {
    private List<Long> events;

    private Boolean pinned;

    @Size(min = 1, max = 50, message = "Имя должно быть от {min} до {max} символов")
    private String title;
}

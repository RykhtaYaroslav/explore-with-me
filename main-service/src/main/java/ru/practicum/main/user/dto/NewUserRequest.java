package ru.practicum.main.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewUserRequest {
    @Email(message = "Неверный формат почты")
    @Size(min = 6, max = 254, message = "Почта должна быть от {min} до {max} символов")
    @NotBlank(message = "Почта не может быть пустой")
    private String email;

    @Size(min = 2, max = 250, message = "Имя должно быть от {min} до {max} символов")
    @NotBlank(message = "Имя не может быть пустым")
    private String name;
}

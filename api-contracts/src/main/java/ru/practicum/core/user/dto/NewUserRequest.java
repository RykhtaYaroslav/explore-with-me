package ru.practicum.core.user.dto;

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
    @Email(message = "Must be a well-formed email address")
    @Size(min = 6, max = 254, message = "Email must be between {min} and {max} characters")
    @NotBlank(message = "Email must not be blank")
    private String email;

    @Size(min = 2, max = 250, message = "Name must be between {min} and {max} characters")
    @NotBlank(message = "Name must not be blank")
    private String name;
}

package ru.practicum.core.user.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.core.user.dto.UserShortDto;
import ru.practicum.core.user.service.internalapi.UserInternalApiService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/interservice/users")
@Validated
public class UserInternalApiController {
    private final UserInternalApiService userInternalApiService;

    @GetMapping("/exist/{id}")
    public boolean existsById(@PathVariable @Positive Long id) {
        return userInternalApiService.existsById(id);
    }

    @GetMapping("/short/{id}")
    public UserShortDto getShortById(@PathVariable @Positive Long id) {
        return userInternalApiService.getShortById(id);
    }

    @GetMapping("/short")
    public List<UserShortDto> getShortByIds(@RequestParam("ids") List<@Positive Long> ids) {
        return userInternalApiService.getShortByIds(ids);
    }
}

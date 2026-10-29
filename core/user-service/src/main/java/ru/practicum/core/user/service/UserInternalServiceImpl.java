package ru.practicum.core.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.core.common.dto.user.UserShortDto;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.user.dto.UserMapper;
import ru.practicum.core.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserInternalServiceImpl implements UserInternalService {
    private final UserRepository repository;
    private final UserMapper userMapper;

    @Override
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    @Override
    public UserShortDto getShortById(Long id) {
        return repository.findById(id)
                .map(userMapper::toUserShortDto)
                .orElseThrow(() -> new NotFoundException(String.format("User with id = %d not found", id)));
    }
}

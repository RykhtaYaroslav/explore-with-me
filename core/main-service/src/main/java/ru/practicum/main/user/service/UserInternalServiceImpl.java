package ru.practicum.main.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.main.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserInternalServiceImpl implements UserInternalService {
    private final UserRepository repository;

    @Override
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }
}

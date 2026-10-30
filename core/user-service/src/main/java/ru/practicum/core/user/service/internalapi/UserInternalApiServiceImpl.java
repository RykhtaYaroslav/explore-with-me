package ru.practicum.core.user.service.internalapi;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.user.dto.UserShortDto;
import ru.practicum.core.user.mapper.UserMapper;
import ru.practicum.core.user.model.User;
import ru.practicum.core.user.repository.UserRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserInternalApiServiceImpl implements UserInternalApiService {
    private final UserRepository repository;
    private final UserMapper userMapper;

    @Override
    public boolean existsById(Long id) {
        return repository.existsById(id);
    }

    @Override
    public UserShortDto getShortById(Long id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("User with id = %d not found", id)));

        return userMapper.toUserShortDto(user, null);
        // TODO implement user rating setting in calling service
    }

    @Override
    public List<UserShortDto> getShortByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        Map<Long, User> usersById = new LinkedHashMap<>();
        repository.findAllById(ids).forEach(user -> usersById.put(user.getId(), user));

        return ids.stream()
                .distinct()
                .map(id -> {
                    User user = usersById.get(id);
                    if (user == null) {
                        throw new NotFoundException(String.format("User with id = %d not found", id));
                    }
                    return userMapper.toUserShortDto(user, null);
                })
                .toList();
    }
}

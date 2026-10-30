package ru.practicum.core.user.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.core.common.dto.user.UserAverageScore;
import ru.practicum.core.common.exception.ConflictException;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.user.dto.NewUserRequest;
import ru.practicum.core.user.dto.UserDto;
import ru.practicum.core.user.interservice.ReviewInterservice;
import ru.practicum.core.user.mapper.UserMapper;
import ru.practicum.core.user.model.User;
import ru.practicum.core.user.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper mapper;
    private final ReviewInterservice reviewInterservice;

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        List<User> users;
        if (ids == null) {
            users = userRepository.getUsersWithoutIds(from, size);
        } else {
            users = userRepository.findAllById(ids);
        }

        List<Long> userIds = users.stream()
                .map(User::getId)
                .toList();

        List<UserAverageScore> result = reviewInterservice.findAverageScoresByTargetIds(userIds);
        Map<Long, Double> ratingMap = result.stream()
                .collect(Collectors.toMap(
                        UserAverageScore::targetId,
                        UserAverageScore::averageScore
                ));

        return users.stream()
                .map(user -> {
                    Double rating = ratingMap.getOrDefault(user.getId(), 0.0);
                    return mapper.toUserDtoOut(user, rating);
                })
                .toList();
    }

    @Override
    public UserDto createUser(NewUserRequest newUser) {
        if (userRepository.existsByEmail(newUser.getEmail())) {
            throw new ConflictException(String.format("A user with email '%s' already exists", newUser.getEmail()));
        }
        User user = mapper.toUser(newUser);
        return mapper.toUserDtoOut(userRepository.save(user), 0.0);
    }

    @Override
    public void deleteUser(long userId) {
        userRepository.findById(userId).orElseThrow(()
                -> new NotFoundException(String.format("User with id = %d was not found", userId)));

        userRepository.deleteById(userId);
    }
}

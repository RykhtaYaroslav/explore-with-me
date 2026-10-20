package ru.practicum.main.user.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.review.repository.UserReviewRepository;
import ru.practicum.main.user.dto.NewUserRequest;
import ru.practicum.main.user.dto.UserDto;
import ru.practicum.main.user.dto.UserMapper;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper mapper;
    private final UserReviewRepository reviewRepository;

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

        List<Object[]> result = reviewRepository.findAverageScoresByTargetIds(userIds);
        Map<Long, Double> ratingMap = result.stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],
                        arr -> (Double) arr[1]
                ));

        return users.stream()
                .map(user -> {
                    UserDto dto = mapper.toUserDtoOut(user);
                    Double rating = ratingMap.getOrDefault(user.getId(), 0.0);
                    dto.setRating(rating);
                    return dto;
                })
                .toList();
    }

    @Override
    public UserDto createUser(NewUserRequest newUser) {
        if (userRepository.existsByEmail(newUser.getEmail())) {
            throw new ConflictException("Юзер с данным email = " + newUser.getEmail() + " уже существует");
        }
        User user = mapper.toUser(newUser);
        return mapper.toUserDtoOut(userRepository.save(user));
    }

    @Override
    public void deleteUser(long userId) {
        userRepository.findById(userId).orElseThrow(()
                -> new NotFoundException("Юзер с id = " + userId + " не найден"));

        userRepository.deleteById(userId);
    }
}
